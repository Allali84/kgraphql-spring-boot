package com.github.ufosightings.dao

import com.github.ufosightings.model.UFOSighting
import org.springframework.stereotype.Component
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.annotation.PostConstruct

@Component
class UFOSightingsImporter(private val sightingsDatabase:UFOSightingRepository) : CSVDataImporter() {

    fun Thread.asResourceStream(): InputStream? = this.contextClassLoader.getResourceAsStream(CSV_FILE_NAME)

    private val CSV_FILE_NAME = "ufo_sightings_2013_2014"
    private val DATE_FORMAT = "M/d/yyyy"

    @PostConstruct
    override fun import() {
        val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
        //val settingsStream = CSV_FILE_NAME.asResourceStream()
        val settingsStream = Thread.currentThread().asResourceStream()
        //val settingsStream = classloader.getResourceAsStream(CSV_FILE_NAME)
        val list = mutableListOf<UFOSighting>()
        importFromCsv(settingsStream) { row ->
            val ufoSighting = UFOSighting(
                    date = LocalDate.parse(row[0], formatter),
                    city = row[1],
                    state = row[2],
                    country = row[3],
                    shape = row[4],
                    duration = row[5].toDouble(),
                    comments = row[6],
                    latitude = row[7].toDouble(),
                    longitude = row[8].toDouble()
            )
            list.add(ufoSighting)
        }
        sightingsDatabase.saveAll(list)
    }
}