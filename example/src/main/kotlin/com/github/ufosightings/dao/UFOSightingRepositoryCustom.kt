package com.github.ufosightings.dao

import com.github.ufosightings.model.CountrySightings

interface UFOSightingRepositoryCustom {
    fun getTopSightings(): MutableList<CountrySightings>

    fun getTopCountrySightings(): MutableList<CountrySightings>
}
