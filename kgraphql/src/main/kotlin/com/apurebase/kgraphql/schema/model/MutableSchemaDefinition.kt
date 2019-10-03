package com.apurebase.kgraphql.schema.model

import com.apurebase.kgraphql.defaultKQLTypeName
import com.apurebase.kgraphql.schema.SchemaException
import com.apurebase.kgraphql.schema.builtin.BUILT_IN_TYPE
import com.apurebase.kgraphql.schema.directive.Directive
import com.apurebase.kgraphql.schema.directive.DirectiveLocation
import com.apurebase.kgraphql.schema.dsl.TypeDSL
import com.apurebase.kgraphql.schema.introspection.TypeKind
import com.apurebase.kgraphql.schema.introspection.__Directive
import com.apurebase.kgraphql.schema.introspection.__EnumValue
import com.apurebase.kgraphql.schema.introspection.__Field
import com.apurebase.kgraphql.schema.introspection.__Schema
import com.apurebase.kgraphql.schema.introspection.__Type
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Intermediate, mutable data structure used to prepare [SchemaDefinition]
 * Performs basic validation (names duplication etc.) when methods for adding schema components are invoked
 */
data class MutableSchemaDefinition (
        private val objects: ArrayList<TypeDef.Object<*>> = arrayListOf(
                TypeDef.Object(__Schema::class.defaultKQLTypeName(), __Schema::class),
                create__TypeDefinition(),
                create__DirectiveDefinition()
        ),
        private val queries: ArrayList<QueryDef<*>> = arrayListOf(),
        private val scalars: ArrayList<TypeDef.Scalar<*>> = arrayListOf(
                BUILT_IN_TYPE.STRING,
                BUILT_IN_TYPE.BOOLEAN,
                BUILT_IN_TYPE.DOUBLE,
                BUILT_IN_TYPE.FLOAT,
                BUILT_IN_TYPE.INT,
                BUILT_IN_TYPE.LONG
        ),
        private val mutations: ArrayList<MutationDef<*>> = arrayListOf(),
        private val enums: ArrayList<TypeDef.Enumeration<*>> = arrayListOf(
                TypeDef.Enumeration (
                        "__" + TypeKind::class.defaultKQLTypeName(),
                        TypeKind::class,
                        enumValues<TypeKind>().map { EnumValueDef(it) }
                ),
                TypeDef.Enumeration (
                        "__" + DirectiveLocation::class.defaultKQLTypeName(),
                        DirectiveLocation::class,
                        enumValues<DirectiveLocation>().map { EnumValueDef(it) }
                )
        ),
        private val unions: ArrayList<TypeDef.Union> = arrayListOf(),
        private val directives: ArrayList<Directive.Partial> = arrayListOf(
                Directive.SKIP,
                Directive.INCLUDE
        ),
        private val inputObjects: ArrayList<TypeDef.Input<*>> = arrayListOf()
) {

        val unionsMonitor : List<TypeDef.Union>
                get() = unions

        fun toSchemaDefinition() : SchemaDefinition {
                val compiledObjects = ArrayList(this.objects)

                unions.forEach { union ->
                        if(union.members.isEmpty()){
                                throw SchemaException("A Union type must define one or more unique member types")
                        }
                        union.members.forEach { member ->
                                validateUnionMember(union, member, compiledObjects)
                        }
                }

                return SchemaDefinition(compiledObjects, queries, scalars, mutations, enums, unions, directives, inputObjects)
        }

        private fun validateUnionMember(union: TypeDef.Union,
                                        member: KClass<*>,
                                        compiledObjects: ArrayList<TypeDef.Object<*>>) {
                if (scalars.any { it.kClass == member } || enums.any { it.kClass == member }) {
                        throw SchemaException(
                                "The member types of a Union type must all be Object base types; " +
                                        "Scalar, Interface and Union types may not be member types of a Union")
                }

                if (member.isSubclassOf(Collection::class)) {
                        throw SchemaException("Collection may not be member type of a Union '${union.name}'")
                }

                if (member.isSubclassOf(Map::class)) {
                        throw SchemaException("Map may not be member type of a Union '${union.name}'")
                }

                if (compiledObjects.none { it.kClass == member }) {
                        compiledObjects.add(TypeDef.Object(member.defaultKQLTypeName(), member))
                }
        }

        fun addQuery(query : QueryDef<*>){
                if(query.checkEqualName(queries)){
                        throw SchemaException("Cannot add query with duplicated name ${query.name}")
                }
                queries.add(query)
        }

        fun addMutation(mutation : MutationDef<*>){
                if(mutation.checkEqualName(mutations)){
                        throw SchemaException("Cannot add mutation with duplicated name ${mutation.name}")
                }
                mutations.add(mutation)
        }

        fun addScalar(scalar: TypeDef.Scalar<*>) = addType(scalar, scalars, "Scalar")

        fun addEnum(enum: TypeDef.Enumeration<*>) = addType(enum, enums, "Enumeration")

        fun addObject(objectType: TypeDef.Object<*>) = addType(objectType, objects, "Object")

        fun addUnion(union: TypeDef.Union) = addType(union, unions, "Union")

        fun addInputObject(input : TypeDef.Input<*>) = addType(input, inputObjects, "Input")

        fun <T : Definition>addType(type: T, target: ArrayList<T>, typeCategory: String){
                if(type.name.startsWith("__")){
                        throw SchemaException("Type name starting with \"__\" are excluded for introspection system")
                }
                if(type.checkEqualName(objects, scalars, unions, enums)){
                        throw SchemaException("Cannot add $typeCategory type with duplicated name ${type.name}")
                }
                target.add(type)
        }

        private fun Definition.checkEqualName(vararg collections: List<Definition>) : Boolean {
                return collections.fold(false, { acc, list -> acc || list.any { it.equalName(this) } })
        }

        private fun Definition.equalName(other: Definition): Boolean {
                return this.name.equals(other.name, true)
        }
}

private fun create__TypeDefinition() = TypeDSL(emptyList(), __Type::class){
        transformation(__Type::fields){ fields: List<__Field>?, includeDeprecated : Boolean? ->
                if (includeDeprecated == true) fields else fields?.filterNot { it.isDeprecated }
        }
        transformation(__Type::enumValues){ enumValues: List<__EnumValue>?, includeDeprecated: Boolean? ->
                if (includeDeprecated == true) enumValues else enumValues?.filterNot { it.isDeprecated }
        }
}.toKQLObject()

private fun create__DirectiveDefinition() = TypeDSL(emptyList(), __Directive::class){
        property<Boolean>("onField"){
                resolver { dir: __Directive ->
                        dir.locations.contains(DirectiveLocation.FIELD)
                }
                deprecate("Use `locations`.")
        }
        property<Boolean>("onFragment"){
                resolver { dir: __Directive -> dir.locations.containsAny (
                        DirectiveLocation.FRAGMENT_SPREAD,
                        DirectiveLocation.FRAGMENT_DEFINITION,
                        DirectiveLocation.INLINE_FRAGMENT)
                }
                deprecate("Use `locations`.")
        }
        property<Boolean>("onOperation"){
                resolver{ dir : __Directive -> dir.locations.containsAny (
                        DirectiveLocation.QUERY,
                        DirectiveLocation.MUTATION,
                        DirectiveLocation.SUBSCRIPTION)
                }
                deprecate("Use `locations`.")
        }
}.toKQLObject()

private fun <T> List<T>.containsAny(vararg elements: T) = elements.filter { this.contains(it) }.any()