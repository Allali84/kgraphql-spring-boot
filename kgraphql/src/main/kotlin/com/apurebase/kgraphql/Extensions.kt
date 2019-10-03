package com.apurebase.kgraphql

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

fun <T : Any> KClass<T>.defaultKQLTypeName() = this.simpleName!!

fun KType.defaultKQLTypeName() = this.jvmErasure.defaultKQLTypeName()

fun String.dropQuotes() : String = if(isLiteral()) drop(1).dropLast(1) else this

fun String.isLiteral() : Boolean = startsWith('\"') && endsWith('\"')

fun KParameter.isNullable() = type.isMarkedNullable

fun KParameter.isNotNullable() = !type.isMarkedNullable

fun KClass<*>.isIterable() = isSubclassOf(Iterable::class)

fun KType.isIterable() = jvmErasure.isIterable()

fun KType.getIterableElementType(): KType? {
    if(!jvmErasure.isIterable()) throw IllegalArgumentException("KType $this is not collection type")
    return arguments.firstOrNull()?.type ?: throw NoSuchElementException("KType $this has no type arguments")
}

fun not(boolean: Boolean) = !boolean