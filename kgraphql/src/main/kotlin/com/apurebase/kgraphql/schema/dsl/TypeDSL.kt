package com.apurebase.kgraphql.schema.dsl

import com.apurebase.kgraphql.defaultKQLTypeName
import com.apurebase.kgraphql.schema.SchemaException
import com.apurebase.kgraphql.schema.model.FunctionWrapper
import com.apurebase.kgraphql.schema.model.PropertyDef
import com.apurebase.kgraphql.schema.model.TypeDef
import com.apurebase.kgraphql.schema.model.Transformation
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1


open class TypeDSL<T : Any>(
        private val supportedUnions: Collection<TypeDef.Union>,
        val kClass: KClass<T>,
        block: TypeDSL<T>.() -> Unit
) : ItemDSL() {

    var name = kClass.defaultKQLTypeName()

    internal val transformationProperties = mutableSetOf<Transformation<T, *>>()

    internal val extensionProperties = mutableSetOf<PropertyDef.Function<T, *>>()

    internal val unionProperties = mutableSetOf<PropertyDef.Union<T>>()

    internal val describedKotlinProperties = mutableMapOf<KProperty1<T, *>, PropertyDef.Kotlin<T, *>>()

    fun <R, E> transformation(kProperty: KProperty1<T, R>, function: suspend (R, E) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R, E, W> transformation(kProperty: KProperty1<T, R>, function: suspend (R, E, W) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R, E, W, Q> transformation(kProperty: KProperty1<T, R>, function: suspend (R, E, W, Q) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R, E, W, Q, A> transformation(kProperty: KProperty1<T, R>, function: suspend (R, E, W, Q, A) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R, E, W, Q, A, S> transformation(kProperty: KProperty1<T, R>, function: suspend (R, E, W, Q, A, S) -> R) {
        transformationProperties.add(Transformation(kProperty, FunctionWrapper.on(function, true)))
    }

    fun <R> property(kProperty: KProperty1<T, R>, block : KotlinPropertyDSL<T, R>.() -> Unit){
        val dsl = KotlinPropertyDSL(kProperty, block)
        describedKotlinProperties[kProperty] = dsl.toKQLProperty()
    }

    fun <R> property(name : String, block : PropertyDSL<T, R>.() -> Unit){
        val dsl = PropertyDSL(name, block)
        extensionProperties.add(dsl.toKQLProperty())
    }

    fun <R> KProperty1<T, R>.configure(block : KotlinPropertyDSL<T, R>.() -> Unit){
        property(this, block)
    }

    fun <R> KProperty1<T, R>.ignore(){
        describedKotlinProperties[this] = PropertyDef.Kotlin(kProperty = this, isIgnored = true)
    }

    fun unionProperty(name : String, block : UnionPropertyDSL<T>.() -> Unit){
        val property = UnionPropertyDSL(name, block)
        val union = supportedUnions.find { property.returnType.typeID.equals(it.name, true) }
                ?: throw SchemaException("Union Type: ${property.returnType.typeID} does not exist")

        unionProperties.add(property.toKQLProperty(union))
    }

    init {
        block()
    }

    internal fun toKQLObject() : TypeDef.Object<T> {
        return TypeDef.Object(
                name = name,
                kClass = kClass,
                kotlinProperties = describedKotlinProperties.toMap(),
                extensionProperties = extensionProperties.toList(),
                unionProperties = unionProperties.toList(),
                transformations = transformationProperties.associate { it.kProperty to it },
                description = description
        )
    }
}