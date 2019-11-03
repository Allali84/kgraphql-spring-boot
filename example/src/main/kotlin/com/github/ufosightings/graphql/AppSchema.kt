package com.github.ufosightings.graphql

import com.apurebase.kgraphql.KGraphQL
import com.apurebase.kgraphql.schema.Schema
import com.apurebase.kgraphql.schema.dsl.subscribe
import com.apurebase.kgraphql.schema.dsl.unsubscribe
import com.github.kgraphql.publisher.Event
import com.github.ufosightings.exception.NotFoundException
import com.github.ufosightings.model.CountrySightings
import com.github.ufosightings.model.UFOSighting
import com.github.ufosightings.model.User
import com.github.ufosightings.model.users
import com.github.ufosightings.publisher.UFOSightingCreatedEvent
import com.github.ufosightings.service.UFOSightingService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.NoSuchElementException

@Configuration
class AppSchema(private val service: UFOSightingService, private val applicationEventPublisher: ApplicationEventPublisher) {

    @Bean
    fun schema(): Schema {
        return KGraphQL.schema {

            configure {
                useDefaultPrettyPrinter = true
            }

            intScalar<BigInteger> {
                serialize = { date -> date.intValueExact() }
                deserialize = { dateString -> BigInteger.valueOf(dateString.toLong()) }
            }

            intScalar<BigDecimal> {
                serialize = { date -> date.intValueExact() }
                deserialize = { dateString -> BigDecimal.valueOf(dateString.toLong()) }
            }

            stringScalar<LocalDate> {
                serialize = { date -> date.toString() }
                deserialize = { dateString -> LocalDate.parse(dateString) }
            }

            query("sightings") {
                description = "Returns a subset of the UFO Sighting records"

                resolver { size: Int? -> service.findAll(PageRequest.of(0, size ?: 10)).toMutableList() }.withArgs {
                    arg<Int> { name = "size"; defaultValue = 10; description = "The number of records to return" }
                }
            }

            query("sighting") {
                description = "Returns a single UFO Sighting record based on the id"

                resolver { id: Int ->
                    try {
                        service.findById(id).get()
                    } catch (e: NoSuchElementException) {
                        throw NotFoundException("Sighting with id: $id does not exist")
                    }
                }
            }

            query("user") {
                description = "Returns a single User based on the id"

                resolver { id: Int ->
                    users.getOrNull(id - 1) ?: throw NotFoundException("User with id: $id does not exist")
                }
            }

            type<User> {
                description = "A User who has reported a UFO sighting"

                property<UFOSighting?>("sighting") {
                    resolver { user -> service.findById(user.id).get() }
                }
            }

            query("topSightings") {
                description = "Returns a list of the top 10 state,country based on the number of sightings"

                resolver { -> service.getTopSightings() }
            }

            query("topCountrySightings") {
                description = "Returns a list of the top 10 countries based on the number of sightings"

                resolver { -> service.getTopCountrySightings() }
            }

            type<CountrySightings> {
                description = "A country sighting; contains total number of occurrences"

                property(CountrySightings::numOccurrences) {
                    description = "The number of occurrences of the sighting"
                }
            }

            val publisher = mutation("createUFOSighting") {
                description = "Adds a new UFO Sighting to the database"

                resolver {
                    input: CreateUFOSightingInput -> service.create(input.toUFOSighting())
                }

            }

            subscription("SubscriptionToCreateUFOSighting") {
                description = "Publish event when a new UFO Sighting is added to the database"

                resolver { subscription: String ->
                    subscribe(subscription, publisher, UFOSighting()) {
                        applicationEventPublisher.publishEvent(UFOSightingCreatedEvent(Event(subscription, it)))
                    }
                }
            }

            subscription("UnsubscriptionToCreateUFOSighting") {
                description = "Unsubscription from the event when a new UFO Sighting is added to the database"

                resolver { subscription: String ->
                    unsubscribe(subscription, publisher, UFOSighting())
                }
            }

            inputType<CreateUFOSightingInput>()

            type<UFOSighting> {
                description = "A UFO sighting"

                property(UFOSighting::date) {
                    description = "The date of the sighting"
                }

                property<User>("user") {
                    resolver {
                        users[(0..2).shuffled().last()]
                    }
                }
            }
        }
    }

}