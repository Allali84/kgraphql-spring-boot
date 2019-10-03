package com.github.kgraphql.controller

import com.apurebase.kgraphql.schema.Schema
import com.github.kgraphql.process.process
import com.github.kgraphql.process.query
import com.github.kgraphql.process.variables
import org.springframework.web.bind.annotation.*

@RestController
class KgraphQLController(private val schema : Schema) {

    @GetMapping("\${spring.kgraphql.host:/graphql}", produces = ["application/json"])
    @ResponseBody
    fun executeGetQuery(@RequestParam("query") request : String): String {
        process(request)
        return schema.execute(query, variables)
    }

    @PostMapping("\${spring.kgraphql.host:/graphql}", produces = ["application/json"])
    @ResponseBody
    fun executePostQuery(@RequestBody(required = true) request : String): String {
        process(request)
        return schema.execute(query, variables)
    }
}