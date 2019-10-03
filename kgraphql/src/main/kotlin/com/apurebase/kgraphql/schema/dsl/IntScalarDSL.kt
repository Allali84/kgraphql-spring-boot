package com.apurebase.kgraphql.schema.dsl

import com.apurebase.kgraphql.schema.SchemaException
import com.apurebase.kgraphql.schema.scalar.IntScalarCoercion
import com.apurebase.kgraphql.schema.scalar.ScalarCoercion
import kotlin.reflect.KClass


class IntScalarDSL<T : Any>(kClass: KClass<T>, block: ScalarDSL<T, Int>.() -> Unit)
    : ScalarDSL<T, Int>(kClass, block){

    override fun createCoercionFromFunctions(): ScalarCoercion<T, Int> {
        return object : IntScalarCoercion<T> {

            val serializeImpl = serialize ?: throw SchemaException(PLEASE_SPECIFY_COERCION)

            val deserializeImpl = deserialize ?: throw SchemaException(PLEASE_SPECIFY_COERCION)

            override fun serialize(instance: T): Int = serializeImpl(instance)

            override fun deserialize(raw: Int): T = deserializeImpl(raw)
        }
    }

}