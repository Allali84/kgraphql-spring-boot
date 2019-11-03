package com.github.kgraphql.autoconfigure

import com.apurebase.kgraphql.schema.Schema
import com.fasterxml.jackson.core.JsonProcessingException
import com.github.kgraphql.controller.KgraphQLController
import com.github.kgraphql.publisher.ApplicationCreatedEvent
import com.github.kgraphql.publisher.Event
import com.github.kgraphql.publisher.EventCreatorPublisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import reactor.core.publisher.Flux
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.util.UriTemplate




@EnableWebFlux
@Configuration
@EnableConfigurationProperties(KGraphQLProperties::class)
class KGraphQLAutoConfiguration(private val graphQLProperties: KGraphQLProperties) {

    @Autowired
    private lateinit var schema : Schema

    @Bean
    fun kgraphQLController(): KgraphQLController {
        return KgraphQLController(schema)
    }

    @Bean
    fun eventCreatorPublisher(): EventCreatorPublisher {
        return EventCreatorPublisher(executor())
    }

    @Bean
    fun corsConfigurer(): WebFluxConfigurer {
        return object : WebFluxConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping(graphQLProperties.cors.mapping)
                        .allowedOrigins(*graphQLProperties.cors.origins)
                .allowedMethods(*graphQLProperties.cors.methods)
                        .allowCredentials(true)
                        .maxAge(graphQLProperties.cors.maxAge)
            }
        }
    }

    @Bean
    fun executor(): Executor {
        return Executors.newSingleThreadExecutor()
    }

    @Bean
    fun handlerMapping(): HandlerMapping {
        return object : SimpleUrlHandlerMapping() {
            init {
                urlMap = Collections.singletonMap("${graphQLProperties.subscription}/{subscription}", webSocketHandler())
                order = 10
            }
        }
    }

    @Bean
    fun webSocketHandlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }

    @Bean
    fun webSocketHandler(): WebSocketHandler {

        val publish = Flux
                .create<ApplicationCreatedEvent>(eventCreatorPublisher())
                .share()

        return WebSocketHandler { session ->
            val param = getParamFromSession(session)
            val messageFlux = publish.map<String> { evt ->
                try {
                    val e = evt.source as Event
                    if (e.subscription == param)
                        e.query
                    else
                        ""
                } catch (e: JsonProcessingException) {
                    throw RuntimeException(e)
                }
            }.map<WebSocketMessage> { str ->
                session.textMessage(str)
            }
            session.send(messageFlux)
        }
    }

    private fun getParamFromSession(session: WebSocketSession): String? {
        val nettySession = session as ReactorNettyWebSocketSession
        val template = UriTemplate("${graphQLProperties.subscription}/{subscription}")
        val parameters = template.match(nettySession.handshakeInfo.uri.path)
        return parameters["subscription"]
    }
}