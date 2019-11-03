package com.github.ufosightings.dao

import com.github.ufosightings.model.CountrySightings
import javax.persistence.EntityManager

class UFOSightingRepositoryCustomImpl(private val entityManager: EntityManager): UFOSightingRepositoryCustom {

    override fun getTopSightings(): MutableList<CountrySightings> {
        val query = entityManager.createNamedQuery("getUFOSighting", CountrySightings::class.java)
        return query.setMaxResults(10).resultList
    }

    override fun getTopCountrySightings(): MutableList<CountrySightings> {
        val query = entityManager.createNamedQuery("getUFOSightingByCountry", CountrySightings::class.java)
        return query.setMaxResults(10).resultList
    }

}