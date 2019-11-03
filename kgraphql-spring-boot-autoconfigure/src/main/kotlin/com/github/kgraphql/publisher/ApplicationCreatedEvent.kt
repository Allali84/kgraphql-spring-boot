package com.github.kgraphql.publisher

import org.springframework.context.ApplicationEvent

open class ApplicationCreatedEvent(source: Event): ApplicationEvent(source)