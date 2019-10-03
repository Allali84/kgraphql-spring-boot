package com.github.ufosightings

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UfoSightingsApplication

fun main(args: Array<String>) {
	runApplication<UfoSightingsApplication>(*args)
}
