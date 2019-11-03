package com.github.kgraphql.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.kgraphql")
data class KGraphQLProperties(
        val host: String = "/graphql",
        val subscription: String = "/subscription",
        val cors: Cors = Cors())

data class Cors (
        val mapping: String = "/**",
        val origins: Array<String> = arrayOf("*"),
        val methods: Array<String> = arrayOf("GET", "POST", "PUT", "DELETE", "HEAD"),
        val maxAge: Long = 3600
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Cors

        if (mapping != other.mapping) return false
        if (!origins.contentEquals(other.origins)) return false
        if (!methods.contentEquals(other.methods)) return false
        if (maxAge != other.maxAge) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mapping.hashCode()
        result = 31 * result + origins.contentHashCode()
        result = 31 * result + methods.contentHashCode()
        result = 31 * result + maxAge.hashCode()
        return result
    }
}
