package com.github.ufosightings.model

import java.math.BigInteger
import java.time.LocalDate
import javax.persistence.*

@SqlResultSetMapping(
        name="CountrySightingsResult",
        classes= [
            ConstructorResult(
                    targetClass = CountrySightings::class,
                    columns=[
                        ColumnResult(name="country"),
                        ColumnResult(name="state"),
                        ColumnResult(name="numOccurrences", type = BigInteger::class)
                        ]
                )
        ]
)
@NamedNativeQueries(
        NamedNativeQuery(
                name="getUFOSighting",
                query="SELECT state, country, COUNT(state) as numOccurrences FROM ufosighting group by state, country order by COUNT(state) DESC",
                resultSetMapping = "CountrySightingsResult"),
        NamedNativeQuery(
                name="getUFOSightingByCountry",
                query="SELECT country, COUNT(country) as numOccurrences FROM ufosighting group by country order by COUNT(country) DESC",
                resultSetMapping = "CountrySightingsResult")
)
@Entity
data class UFOSighting(
        @Id
        @GeneratedValue(strategy= GenerationType.IDENTITY)
        var id: Int = -1,
        var date: LocalDate = LocalDate.now(),
        var city: String? = "",
        var state: String? = "",
        var country: String? = "",
        var shape: String? = "",
        var duration: Double = 0.0,
        var comments: String? = "",
        var latitude: Double = 0.0,
        var longitude: Double = 0.0
)