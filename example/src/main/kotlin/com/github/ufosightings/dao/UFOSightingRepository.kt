package com.github.ufosightings.dao

import com.github.ufosightings.model.UFOSighting
import org.springframework.data.jpa.repository.JpaRepository

interface UFOSightingRepository : JpaRepository<UFOSighting, Int>, UFOSightingRepositoryCustom