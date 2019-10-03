package com.apurebase.kgraphql.demo

import com.apurebase.kgraphql.KGraphQL

fun main(args : Array<String>) {
    val schema = KGraphQL.schema {
        query("hello") {
            resolver { name : String -> "Hello, $name" }
        }
    }

    //prints '{"data":{"hello":"Hello, Ted Mosby"}}'
    println(schema.execute("{hello(name : \"Ted Mosby\")}"))
}
