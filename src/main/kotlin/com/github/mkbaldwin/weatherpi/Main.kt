package com.github.mkbaldwin.weatherpi

import com.github.mkbaldwin.weatherpi.conf.Configuration
import com.github.mkbaldwin.weatherpi.json.deserializeIncomingObservation
import com.github.mkbaldwin.weatherpi.persistence.*
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream
import java.time.Instant
import java.util.*
import kotlin.system.exitProcess


// #####################################################################################################################
// #####################################################################################################################
// #####################################################################################################################
fun main(args: Array<String>) {
    val configuration = loadConfiguration(args)
    val influxOperations = InfluxOperations(configuration)

    while (true) {
        val input = readLine()
        if (input != null) {
            println("~~~ $input")
            processObservation(input, influxOperations)
        }

        // Keep the loop from running so tightly and consuming the CPU core. Since this is probably going on a PI Zero W
        // it is important not to consume the only CPU core too much. Not sure if there is a better way to handle this
        // when reading from STDIN.
        Thread.sleep(100L)
    }
}

// #####################################################################################################################
// #####################################################################################################################
// #####################################################################################################################
private fun loadConfiguration(args: Array<String>): Configuration {
    //Configuration file should be the first parameter provided
    if (args.isEmpty()) {
        println("Parameter 1 must be path to configuration file.")
        exitProcess(1)
    }

    val inputStream = FileInputStream(args[0])
    return with(Properties()) {
        load(inputStream)
        Configuration(
            getProperty("influx.host"),
            getProperty("influx.bucket"),
            getProperty("influx.org"),
            getProperty("influx.token")
        )
    }
}

// #####################################################################################################################
// #####################################################################################################################
// #####################################################################################################################
private fun processObservation(observationJson: String, influxOperations: InfluxOperations) {
    println("Received: observationJson")
    val observation = deserializeIncomingObservation(observationJson)
    val time = Instant.now().toEpochMilli()

    runBlocking {
        // It seems that sometimes the receiver can report in C or F (maybe some setting?) either way
        // we will accept and process the data.
        if (observation.temperatureC != null || observation.temperatureF != null) {
            influxOperations.recordTemperature(observation.temperatureC, observation.temperatureF, time)
        }

        // The sensor seems to be able to report either km/h or mi/h so handle both
        if (observation.windSpeedKmh != null || observation.windSpeedMph != null) {
            influxOperations.recordWind(observation.windSpeedKmh, observation.windSpeedMph, observation.windDirection, time)
        }

        observation.humidity?.let {
            influxOperations.recordHumidity(it, time)
        }

        observation.rainMm?.let { it ->
            influxOperations.recordRain(it, time)
        }
    }
}