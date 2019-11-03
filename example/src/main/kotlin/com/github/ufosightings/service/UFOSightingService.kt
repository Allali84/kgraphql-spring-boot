package com.github.ufosightings.service

import com.github.ufosightings.dao.UFOSightingRepository
import com.github.ufosightings.model.CountrySightings
import com.github.ufosightings.model.UFOSighting
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.*

@Service
class UFOSightingService(private val repository: UFOSightingRepository) {


    fun create(uFOSighting: UFOSighting): UFOSighting {
        return this.repository
                .save(uFOSighting)
    }

    fun findAll(pageable: PageRequest): Iterable<UFOSighting> {
        return repository.findAll(pageable)
    }

    fun findById(id: Int): Optional<UFOSighting> {
        return repository.findById(id)
    }

    fun getTopSightings(): MutableList<CountrySightings> {
        return repository.getTopSightings()
    }

    fun getTopCountrySightings(): MutableList<CountrySightings> {
        return repository.getTopSightings()
    }
}