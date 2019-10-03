package com.github.kgraphql.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.kgraphql")
data class KGraphQLProperties(val host: String = "/graphql")