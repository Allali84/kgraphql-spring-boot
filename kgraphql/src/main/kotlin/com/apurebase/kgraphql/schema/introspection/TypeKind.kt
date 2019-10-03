package com.apurebase.kgraphql.schema.introspection


enum class TypeKind {
    SCALAR,
    OBJECT,
    INTERFACE,
    UNION,
    ENUM,
    INPUT_OBJECT,

    //wrapper types
    LIST,
    NON_NULL
}