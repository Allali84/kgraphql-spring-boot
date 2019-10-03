package com.github.ufosightings.model

import java.math.BigInteger

data class CountrySightings(
        var state: String = "",
        var country: String = "",
        var numOccurrences: BigInteger = BigInteger.ZERO
)