package com.github.ufosightings.graphql

import com.apurebase.kgraphql.KGraphQL
import com.apurebase.kgraphql.schema.Schema
import com.github.ufosightings.dao.UFOSightingRepository
import com.github.ufosightings.exception.NotFoundException
import com.github.ufosightings.model.CountrySightings
import com.github.ufosightings.model.UFOSighting
import com.github.ufosightings.model.User
import com.github.ufosightings.model.users
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.PageRequest
import java.math.BigInteger
import java.time.LocalDate
import java.util.NoSuchElementException

@Configuration
class AppSchema(private val repository: UFOSightingRepository) {

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

            stringScalar<LocalDate> {
                serialize = { date -> date.toString() }
                deserialize = { dateString -> LocalDate.parse(dateString) }
            }

            query("sightings") {
                description = "Returns a subset of the UFO Sighting records"

                resolver { size: Int? -> repository.findAll(PageRequest.of(0,size ?: 10)).toMutableList() }.withArgs {
                    arg<Int> { name = "size"; defaultValue = 10; description = "The number of records to return" }
                }
            }

            query("sighting") {
                description = "Returns a single UFO Sighting record based on the id"

                resolver {
                    id: Int -> try{
                    repository.findById(id).get()
                } catch (e: NoSuchElementException){
                    throw NotFoundException("Sighting with id: $id does not exist")
                }
                }
            }

            query("user") {
                description = "Returns a single User based on the id"

                resolver { id: Int -> users.getOrNull(id-1) ?: throw NotFoundException("User with id: $id does not exist") }
            }

            type<User> {
                description = "A User who has reported a UFO sighting"

                property<UFOSighting?>("sighting") {
                    resolver { user -> repository.findById(user.id).get() }
                }
            }

            query("topSightings") {
                description = "Returns a list of the top 10 state,country based on the number of sightings"

                resolver{ -> repository.getTopSightings()}
            }

            query("topCountrySightings") {
                description = "Returns a list of the top 10 countries based on the number of sightings"

                resolver{-> repository.getTopCountrySightings()}
            }

            type<CountrySightings> {
                description = "A country sighting; contains total number of occurrences"

                property(CountrySightings::numOccurrences) {
                    description = "The date of the sighting"
                }
            }

            /*mutation("createUFOSighting") {
                description = "Adds a new UFO Sighting to the database"

                resolver { input: CreateUFOSightingInput -> repository.save(input.toUFOSighting()) }
            }

            inputType<CreateUFOSightingInput>()*/

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