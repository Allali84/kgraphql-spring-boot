package com.apurebase.kgraphql.schema.structure2

import com.apurebase.kgraphql.Context
import com.apurebase.kgraphql.configuration.SchemaConfiguration
import com.apurebase.kgraphql.defaultKQLTypeName
import com.apurebase.kgraphql.getIterableElementType
import com.apurebase.kgraphql.isIterable
import com.apurebase.kgraphql.schema.DefaultSchema
import com.apurebase.kgraphql.schema.SchemaException
import com.apurebase.kgraphql.schema.directive.Directive
import com.apurebase.kgraphql.schema.introspection.SchemaProxy
import com.apurebase.kgraphql.schema.introspection.TypeKind
import com.apurebase.kgraphql.schema.introspection.__Schema
import com.apurebase.kgraphql.schema.model.BaseOperationDef
import com.apurebase.kgraphql.schema.model.FunctionWrapper
import com.apurebase.kgraphql.schema.model.InputValueDef
import com.apurebase.kgraphql.schema.model.PropertyDef
import com.apurebase.kgraphql.schema.model.QueryDef
import com.apurebase.kgraphql.schema.model.SchemaDefinition
import com.apurebase.kgraphql.schema.model.Transformation
import com.apurebase.kgraphql.schema.model.TypeDef
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

@Suppress("UNCHECKED_CAST")
class SchemaCompilation(
        val configuration : SchemaConfiguration,
        val definition : SchemaDefinition
){

    private val queryTypeProxies = mutableMapOf<KClass<*>, TypeProxy>()

    private val inputTypeProxies = mutableMapOf<KClass<*>, TypeProxy>()

    private val unions = mutableListOf<Type.Union>()

    private val enums = definition.enums.associate { enum -> enum.kClass to enum.toEnumType() }

    private val scalars = definition.scalars.associate { scalar -> scalar.kClass to scalar.toScalarType() }

    private val schemaProxy = SchemaProxy()

    private val contextType = Type._Context()

    private enum class TypeCategory {
        INPUT, QUERY
    }

    fun perform(): DefaultSchema {
        val queryType = handleQueries()
        val mutationType = handleMutations()
        definition.objects.forEach { handleObjectType(it.kClass) }
        definition.inputObjects.forEach { handleInputType(it.kClass) }

        queryTypeProxies.forEach { (kClass, typeProxy) ->
            introspectPossibleTypes(kClass, typeProxy)
            introspectInterfaces(kClass, typeProxy)
        }

        val model =  SchemaModel (
                query = queryType,
                mutation = if (mutationType.fields!!.isEmpty()) null else mutationType,

                enums = enums,
                scalars = scalars,
                unions = unions,

                queryTypes = queryTypeProxies + enums + scalars,
                inputTypes = inputTypeProxies + enums + scalars,
                allTypes = queryTypeProxies.values
                        + inputTypeProxies.values
                        + enums.values
                        + scalars.values
                        + unions.distinctBy(Type.Union::name),
                directives = definition.directives.map { handlePartialDirective(it) }
        )
        val schema = DefaultSchema(configuration, model)
        schemaProxy.proxiedSchema = schema
        return schema
    }

    private fun introspectPossibleTypes(kClass: KClass<*>, typeProxy: TypeProxy) {
        val proxied = typeProxy.proxied
        if(proxied is Type.Interface<*>){
            val possibleTypes = queryTypeProxies.filter { (otherKClass, otherTypeProxy) ->
                otherTypeProxy.kind == TypeKind.OBJECT && otherKClass != kClass && otherKClass.isSubclassOf(kClass)
            }.values.toList()

            typeProxy.proxied = proxied.withPossibleTypes(possibleTypes)
        }
    }

    private fun introspectInterfaces(kClass: KClass<*>, typeProxy: TypeProxy) {
        val proxied = typeProxy.proxied
        if(proxied is Type.Object<*>){
            val interfaces = queryTypeProxies.filter { (otherKClass, otherTypeProxy) ->
                otherTypeProxy.kind == TypeKind.INTERFACE && otherKClass != kClass && kClass.isSubclassOf(otherKClass)
            }.values.toList()

            typeProxy.proxied = proxied.withInterfaces(interfaces)
        }
    }

    private fun handlePartialDirective(directive: Directive.Partial) : Directive {
        val inputValues = handleInputValues(directive.name, directive.execution, emptyList())
        return directive.toDirective(inputValues)
    }

    private fun handleQueries() : Type {
        return Type.OperationObject("Query", "Query object",
            fields = definition.queries.map { handleOperation(it) } + introspectionSchemaQuery() + introspectionTypeQuery()
        )
    }

    private fun handleMutations() : Type {
        return Type.OperationObject("Mutation", "Mutation object", definition.mutations.map { handleOperation(it) })
    }

    @Suppress("USELESS_CAST") // We are casting as __Schema so we don't get proxied types. https://github.com/aPureBase/KGraphQL/issues/45
    private fun introspectionSchemaQuery() = handleOperation(
        QueryDef("__schema", FunctionWrapper.on<__Schema> { schemaProxy as __Schema })
    )

    private fun introspectionTypeQuery() = handleOperation(
        QueryDef("__type", FunctionWrapper.on {
            name : String -> schemaProxy.findTypeByName(name)
        })
    )

    private fun handleOperation(operation : BaseOperationDef<*, *>) : Field {
        val returnType = handlePossiblyWrappedType(operation.kFunction.returnType, TypeCategory.QUERY)
        val inputValues = handleInputValues(operation.name, operation, operation.inputValues)
        return Field.Function(operation, returnType, inputValues)
    }

    private fun handleUnionProperty(unionProperty: PropertyDef.Union<*>) : Field {
        val inputValues = handleInputValues(unionProperty.name, unionProperty, unionProperty.inputValues)
        val type = handleUnionType(unionProperty.union)

        return Field.Union(unionProperty, unionProperty.nullable, type, inputValues)
    }

    private fun handlePossiblyWrappedType(kType : KType, typeCategory: TypeCategory) : Type = when {
        kType.isIterable() -> handleCollectionType(kType, typeCategory)
        kType.jvmErasure == Context::class && typeCategory == TypeCategory.INPUT -> contextType
        kType.jvmErasure == Context::class && typeCategory == TypeCategory.QUERY -> throw SchemaException("Context type cannot be part of schema")
        kType.arguments.isNotEmpty() -> throw SchemaException("Generic types are not supported by GraphQL, found $kType")
        else -> handleSimpleType(kType, typeCategory)
    }

    private fun handleCollectionType(kType: KType, typeCategory: TypeCategory): Type {
        val type = kType.getIterableElementType()
                ?: throw SchemaException("Cannot handle collection without element type")

        val nullableListType = Type.AList(handleSimpleType(type, typeCategory))
        return applyNullability(kType, nullableListType)
    }

    private fun handleSimpleType(kType: KType, typeCategory: TypeCategory): Type {
        val simpleType = handleRawType(kType.jvmErasure, typeCategory)
        return applyNullability(kType, simpleType)
    }

    private fun applyNullability(kType: KType, simpleType: Type): Type {
        if (!kType.isMarkedNullable) {
            return Type.NonNull(simpleType)
        } else {
            return simpleType
        }
    }

    private fun handleRawType(kClass: KClass<*>, typeCategory: TypeCategory) : Type {

        if(kClass == Context::class) throw SchemaException("Context type cannot be part of schema")

        val cachedInstances = when(typeCategory) {
            TypeCategory.QUERY -> queryTypeProxies
            TypeCategory.INPUT -> inputTypeProxies
        }

        val typeCreator = when(typeCategory){
            TypeCategory.QUERY -> this::handleObjectType
            TypeCategory.INPUT -> this::handleInputType
        }

        return cachedInstances[kClass]
                ?: enums[kClass]
                ?: scalars[kClass]
                ?: typeCreator (kClass)
    }

    private fun handleObjectType(kClass: KClass<*>) : Type {
        assertValidObjectType(kClass)
        val objectDefs = definition.objects.filter { it.kClass.isSuperclassOf(kClass) }
        val objectDef = objectDefs.find { it.kClass == kClass } ?: TypeDef.Object(kClass.defaultKQLTypeName(), kClass)

        //treat introspection types as objects -> adhere to reference implementation behaviour
        val kind = if(kClass.isFinal || objectDef.name.startsWith("__")) TypeKind.OBJECT else TypeKind.INTERFACE

        val objectType = if(kind == TypeKind.OBJECT) Type.Object(objectDef) else Type.Interface(objectDef)
        val typeProxy = TypeProxy(objectType)
        queryTypeProxies.put(kClass, typeProxy)

        val allKotlinProperties = objectDefs.fold(emptyMap<String, PropertyDef.Kotlin<*, *>>(),
                { acc, def -> acc + def.kotlinProperties.mapKeys { entry -> entry.key.name } })
        val allTransformations= objectDefs.fold(emptyMap<String, Transformation<*, *>>(),
                { acc, def -> acc + def.transformations.mapKeys { entry -> entry.key.name } })

        val kotlinFields = kClass.memberProperties
                .filter { field -> field.visibility == KVisibility.PUBLIC }
                .filterNot { field ->  objectDefs.any { it.isIgnored(field.name) } }
                .map { property -> handleKotlinProperty (
                        kProperty = property,
                        kqlProperty = allKotlinProperties[property.name],
                        transformation = allTransformations[property.name]
                ) }

        val extensionFields = objectDefs.flatMap(TypeDef.Object<*>::extensionProperties).map { property -> handleOperation(property) }

        val unionFields = objectDefs.flatMap(TypeDef.Object<*>::unionProperties).map { property -> handleUnionProperty(property) }

        val typenameResolver: suspend (Any) -> String? = { value: Any ->
            schemaProxy.typeByKClass(value.javaClass.kotlin)?.name ?: typeProxy.name
        }

        val __typenameField = handleOperation (
                PropertyDef.Function<Nothing, String?> ("__typename", FunctionWrapper.on(typenameResolver, true))
        )

        val declaredFields = kotlinFields + extensionFields + unionFields

        if(declaredFields.isEmpty()){
            throw SchemaException("An Object type must define one or more fields. Found none on type ${objectDef.name}")
        }

        declaredFields.find { it.name.startsWith("__") }?.let { field ->
            throw SchemaException("Illegal name '${field.name}'. Names starting with '__' are reserved for introspection system")
        }

        val allFields = declaredFields + __typenameField
        typeProxy.proxied = if(kind == TypeKind.OBJECT) Type.Object(objectDef, allFields) else Type.Interface(objectDef, allFields)
        return typeProxy
    }

    private fun handleInputType(kClass: KClass<*>) : Type {
        assertValidObjectType(kClass)

        val inputObjectDef = definition.inputObjects.find { it.kClass == kClass } ?: TypeDef.Input(kClass.defaultKQLTypeName(), kClass)
        val objectType = Type.Input(inputObjectDef)
        val typeProxy = TypeProxy(objectType)
        inputTypeProxies.put(kClass, typeProxy)

        val fields = kClass.memberProperties.map { property -> handleKotlinInputProperty(property) }

        typeProxy.proxied = Type.Input(inputObjectDef, fields)
        return typeProxy
    }

    private fun handleInputValues(operationName : String, operation: FunctionWrapper<*>, inputValues: List<InputValueDef<*>>) : List<InputValue<*>> {
        val invalidInputValues = inputValues
                .map { it.name }
                .filterNot { it in operation.argumentsDescriptor.keys }

        if(invalidInputValues.isNotEmpty()){
            throw SchemaException("Invalid input values on $operationName: $invalidInputValues")
        }

        return operation.argumentsDescriptor.map { (name, kType) ->
            val kqlInput = inputValues.find { it.name == name } ?: InputValueDef(kType.jvmErasure, name)
            val inputType = handlePossiblyWrappedType(kType, TypeCategory.INPUT)
            InputValue(kqlInput, inputType)
        }
    }

    private fun handleUnionType(union : TypeDef.Union) : Type.Union {
        val possibleTypes = union.members.map {
            handleRawType(it, TypeCategory.QUERY)
        }

        val invalidPossibleTypes = possibleTypes.filterNot { it.kind == TypeKind.OBJECT }
        if(invalidPossibleTypes.isNotEmpty()){
            throw SchemaException("Invalid union type members")
        }

        val unionType = Type.Union(union, possibleTypes)
        unions.add(unionType)
        return unionType
    }

    private fun handleKotlinInputProperty(kProperty: KProperty1<*, *>) : InputValue<*> {
        val type = handlePossiblyWrappedType(kProperty.returnType, TypeCategory.INPUT)
        return InputValue(InputValueDef(kProperty.returnType.jvmErasure, kProperty.name), type)
    }

    private fun <T : Any, R> handleKotlinProperty (
            kProperty: KProperty1<T, R>,
            kqlProperty: PropertyDef.Kotlin<*, *>?,
            transformation: Transformation<*, *>?
    ) : Field.Kotlin<*, *> {
        val returnType = handlePossiblyWrappedType(kProperty.returnType, TypeCategory.QUERY)
        val inputValues = if(transformation != null){
            handleInputValues("$kProperty transformation", transformation.transformation, emptyList())
        } else {
            emptyList()
        }

        val actualKqlProperty = kqlProperty ?: PropertyDef.Kotlin(kProperty)

        return Field.Kotlin (
                kql = actualKqlProperty as PropertyDef.Kotlin<T, R>,
                returnType = returnType,
                arguments = inputValues,
                transformation = transformation as Transformation<T, R>?
        )
    }
}
