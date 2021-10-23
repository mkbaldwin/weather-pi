package com.github.mkbaldwin.weatherpi

import com.github.mkbaldwin.weatherpi.json.deserializeIncomingObservation
import com.github.mkbaldwin.weatherpi.persistence.recordHumidity
import com.github.mkbaldwin.weatherpi.persistence.recordRain
import com.github.mkbaldwin.weatherpi.persistence.recordTemperature
import com.github.mkbaldwin.weatherpi.persistence.recordWind
import kotlinx.coroutines.runBlocking
import java.time.Instant


fun main() {
    while (true) {
        val input = readLine()
        if (input != null) {
            println("~~~ $input")
            processObservation(input)
        }

        Thread.sleep(100L)
    }
}

private fun processObservation(observationJson: String) {
    println("Received: observationJson")
    val observation = deserializeIncomingObservation(observationJson)
    val time = Instant.now().toEpochMilli()

    runBlocking {
        // It seems that sometimes the receiver can report in C or F (maybe some setting?) either way
        // we will accept and process the data.
        if (observation.temperatureC != null || observation.temperatureF != null) {
            recordTemperature(observation.temperatureC, observation.temperatureF, time)
        }

        observation.windSpeed?.let { it ->
            recordWind(it, observation.windDirection, time)
        }

        observation.humidity?.let {
            recordHumidity(it, time)
        }

        observation.rain?.let { it ->
            recordRain(it, time)
        }
    }
}