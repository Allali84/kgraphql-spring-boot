package com.apurebase.kgraphql.schema.builtin

import com.apurebase.kgraphql.RequestException
import com.apurebase.kgraphql.defaultKQLTypeName
import com.apurebase.kgraphql.dropQuotes
import com.apurebase.kgraphql.isLiteral
import com.apurebase.kgraphql.schema.model.TypeDef
import com.apurebase.kgraphql.schema.scalar.StringScalarCoercion


private val STRING_DESCRIPTION =
        "The String scalar type represents textual data, represented as UTF‐8 character sequences"

private val INT_DESCRIPTION =
        "The Int scalar type represents a signed 32‐bit numeric non‐fractional value"

private val LONG_DESCRIPTION =
        "The Long scalar type represents a signed 64‐bit numeric non‐fractional value"

private val FLOAT_DESCRIPTION =
        "The Float scalar type represents signed double‐precision fractional values as specified by IEEE 754"

private val BOOLEAN_DESCRIPTION =
        "The Boolean scalar type represents true or false"

/**
 * These scalars are created only for sake of documentation in introspection, not during execution
 */
object BUILT_IN_TYPE {

    val STRING = TypeDef.Scalar(String::class.defaultKQLTypeName(), String::class, STRING_COERCION, STRING_DESCRIPTION)

    val INT = TypeDef.Scalar(Int::class.defaultKQLTypeName(), Int::class, INT_COERCION, INT_DESCRIPTION)

    //GraphQL does not differ float and double, treat double like float
    val DOUBLE = TypeDef.Scalar(Float::class.defaultKQLTypeName(), Double::class, DOUBLE_COERCION, FLOAT_DESCRIPTION)

    val FLOAT = TypeDef.Scalar(Float::class.defaultKQLTypeName(), Float::class, FLOAT_COERCION, FLOAT_DESCRIPTION)

    val BOOLEAN = TypeDef.Scalar(Boolean::class.defaultKQLTypeName(), Boolean::class, BOOLEAN_COERCION, BOOLEAN_DESCRIPTION)

    val LONG = TypeDef.Scalar(Long::class.defaultKQLTypeName(), Long::class, LONG_COERCION, LONG_DESCRIPTION)
}

object STRING_COERCION : StringScalarCoercion<String>{
    override fun serialize(instance: String): String = instance

    override fun deserialize(raw: String): String {
        if(raw.isLiteral()) {
            return raw.dropQuotes()
        } else {
            throw RequestException("Cannot coerce string constant $raw, expected string literal")
        }
    }
}

object DOUBLE_COERCION : StringScalarCoercion<Double>{
    override fun serialize(instance: Double): String = instance.toString()

    override fun deserialize(raw: String): Double {
        if(raw.isLiteral()) {
            throw RequestException("Cannot coerce string literal, expected numeric string constant")
        } else {
            return raw.toDouble()
        }
    }
}

object FLOAT_COERCION : StringScalarCoercion<Float>{
    override fun serialize(instance: Float): String = instance.toDouble().toString()

    override fun deserialize(raw: String): Float = DOUBLE_COERCION.deserialize(raw).toFloat()
}

object INT_COERCION : StringScalarCoercion<Int>{
    override fun serialize(instance: Int): String = instance.toString()

    override fun deserialize(raw: String): Int {
        if(raw.isLiteral()) {
            throw RequestException("Cannot coerce string literal, expected numeric string constant")
        } else {
            return raw.toInt()
        }
    }
}

object LONG_COERCION : StringScalarCoercion<Long> {
    override fun serialize(instance: Long): String = instance.toString()

    override fun deserialize(raw: String): Long {
        if(raw.isLiteral()) {
            throw RequestException("Cannot coerce string literal, expected numeric string constant")
        } else {
            return raw.toLong()
        }
    }
}


object BOOLEAN_COERCION : StringScalarCoercion<Boolean> {
    override fun serialize(instance: Boolean): String = instance.toString()

    override fun deserialize(raw: String): Boolean {
        if(raw.isLiteral()) {
            throw RequestException("Cannot coerce string literal, expected numeric string constant")
        } else {
            return when {
            //custom parsing, because String#toBoolean() returns false for any input != true
                raw.equals("true", true) -> true
                raw.equals("false", true) -> false
                else -> throw IllegalArgumentException("$raw does not represent valid Boolean value")
            }
        }
    }
}