package com.apurebase.kgraphql.schema.dsl

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.apurebase.kgraphql.configuration.SchemaConfiguration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers


class SchemaConfigurationDSL {
    var useDefaultPrettyPrinter: Boolean = false
    var useCachingDocumentParser: Boolean = true
    var objectMapper: ObjectMapper = jacksonObjectMapper()
    var documentParserCacheMaximumSize: Long = 1000L
    var acceptSingleValueAsArray: Boolean = true
    var coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default

    internal fun update(block: SchemaConfigurationDSL.() -> Unit) = block()

    internal fun build(): SchemaConfiguration {
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, acceptSingleValueAsArray)
        return SchemaConfiguration(
                useCachingDocumentParser,
                documentParserCacheMaximumSize,
                objectMapper,
                useDefaultPrettyPrinter,
                coroutineDispatcher
        )
    }
}
