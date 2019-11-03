package com.github.kgraphql.controller

import com.apurebase.kgraphql.schema.Schema
import com.github.kgraphql.handler.handle
import com.github.kgraphql.handler.query
import com.github.kgraphql.handler.variables
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*


@RestController
class KgraphQLController(private val schema : Schema) {

    @GetMapping("\${spring.kgraphql.host:/graphql}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun executeGetQuery(@RequestParam("query") request : String): String {
        handle(request)
        return schema.execute(query, variables)
    }

    @PostMapping("\${spring.kgraphql.host:/graphql}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun executePostQuery(@RequestBody(required = true) request : String): String {
        handle(request)
        return schema.execute(query, variables)
    }
}