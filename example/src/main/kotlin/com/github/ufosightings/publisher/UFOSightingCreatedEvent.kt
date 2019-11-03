package com.github.ufosightings.publisher

import com.github.kgraphql.publisher.ApplicationCreatedEvent
import com.github.kgraphql.publisher.Event

class UFOSightingCreatedEvent(source: Event): ApplicationCreatedEvent(source)