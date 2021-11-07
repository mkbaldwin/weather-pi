package com.github.mkbaldwin.weatherpi

import com.github.mkbaldwin.weatherpi.conf.Configuration
import com.github.mkbaldwin.weatherpi.json.deserializeIncomingObservation
import com.github.mkbaldwin.weatherpi.persistence.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors


// #####################################################################################################################
// #####################################################################################################################
// #####################################################################################################################
fun main(args: Array<String>): Unit = runBlocking {
    val configuration = loadConfiguration(args)
    val influxOperations = InfluxOperations(configuration)

    // If we have a barometric pressure sensor then we want to start a separate coroutine to read that sensor
    if (configuration.barometerEnable) {
        launch {
            println("Enabling barometric pressure sensor. (read interval ${configuration.barometerIntervalMs}ms)")
            while (true) {
                println("Read barometer")
                delay(configuration.barometerIntervalMs)
                println("after")
            }
        }

    }

    // Launch a coroutine for processing the observations, because this will need to read from STDIN (a blocking
    // operation) we want to force the use of a separate thread so that it doesn't interfere with other operations.
    launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
        while (true) {
            val input = readLine()
            if (input != null) {
                processObservation(input, influxOperations)
            }

            // Keep the loop from running so tightly and consuming the CPU core. Since this is probably going on a PI Zero W
            // it is important not to consume the only CPU core too much. Not sure if there is a better way to handle this
            // when reading from STDIN.
            delay(100L)
        }
    }
}

// #####################################################################################################################
// #####################################################################################################################
// #####################################################################################################################
private fun loadConfiguration(args: Array<String>): Configuration {
    // Configure the command line argument parser
    val parser = ArgParser("example")
    val configFile by parser.option(
        ArgType.String,
        shortName = "c",
        description = "Path to configuration file."
    ).required()
    val barometerEnabled by parser.option(
        ArgType.Boolean,
        shortName = "b",
        description = "Enable reading barometric pressure from attached sensor."
    )
    val barometerInterval by parser.option(
        ArgType.Int,
        description = "Interval for reading the barometer in seconds."
    ).default(300)

    parser.parse(args)
    // Load the configuration file from the specified path.
    val inputStream = FileInputStream(configFile)
    return with(Properties()) {
        load(inputStream)
        Configuration(
            getProperty("influx.host"),
            getProperty("influx.bucket"),
            getProperty("influx.org"),
            getProperty("influx.token"),
            barometerEnabled ?: false,
            (barometerInterval * 1000).toLong()
        )
    }
}

// #####################################################################################################################
// #####################################################################################################################
// #####################################################################################################################
private suspend fun processObservation(observationJson: String, influxOperations: InfluxOperations) {
    println("Received: $observationJson")
    val observation = deserializeIncomingObservation(observationJson)
    val time = Instant.now().toEpochMilli()

    if (observation == null) {
        println("Received invalid observation data: $observationJson")
        return
    }


    // It seems that sometimes the receiver can report in C or F (maybe some setting?) either way
    // we will accept and process the data.
    if (observation.temperatureC != null || observation.temperatureF != null) {
        influxOperations.recordTemperature(observation.temperatureC, observation.temperatureF, time)
    }

    // The sensor seems to be able to report either km/h or mi/h so handle both
    if (observation.windSpeedKmh != null || observation.windSpeedMph != null) {
        influxOperations.recordWind(
            observation.windSpeedKmh,
            observation.windSpeedMph,
            observation.windDirection,
            time
        )
    }

    observation.humidity?.let {
        influxOperations.recordHumidity(it, time)
    }

    if (observation.rainMm != null && observation.rainIn != null) {
        influxOperations.recordRain(observation.rainMm, observation.rainIn, time)
    }

}