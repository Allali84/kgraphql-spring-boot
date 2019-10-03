package com.github.kgraphql.autoconfigure

import com.apurebase.kgraphql.schema.Schema
import com.github.kgraphql.controller.KgraphQLController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean


@Configuration
@EnableConfigurationProperties(KGraphQLProperties::class)
class KGraphQLAutoConfiguration(private val graphQLProperties: KGraphQLProperties) {

    @Autowired
    private lateinit var schema : Schema

    @Bean
    fun kgraphQLController(): KgraphQLController {
        return KgraphQLController(schema)
    }
}