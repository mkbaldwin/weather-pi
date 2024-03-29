package com.github.mkbaldwin.weatherpi

import com.github.mkbaldwin.weatherpi.conf.Configuration
import com.github.mkbaldwin.weatherpi.json.deserializeIncomingBarometer
import com.github.mkbaldwin.weatherpi.json.deserializeIncomingObservation
import com.github.mkbaldwin.weatherpi.persistence.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.FileInputStream
import java.time.Instant
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

// #####################################################################################################################
// #####################################################################################################################
// #####################################################################################################################
fun main(args: Array<String>): Unit = runBlocking {
    logger.info { "Starting application..." }
    val configuration = loadConfiguration(args)
    val influxOperations = InfluxOperations(configuration)

    // If we have a barometric pressure sensor then we want to start a separate coroutine to read that sensor
    if (configuration.barometerEnable) {
        launch {
            logger.info { "Enabling barometric pressure sensor. (read interval ${configuration.barometerIntervalMs}ms)" }
            while (true) {
                logger.info { "Read barometer" }

                val process = ProcessBuilder(configuration.barometerCommand).start()
                process.inputStream.reader(Charsets.UTF_8).use {
                    processBarometerObservation(it.readText(), influxOperations)
                }
                withContext(Dispatchers.IO) {
                    process.waitFor(30, TimeUnit.SECONDS)
                }

                delay(configuration.barometerIntervalMs)
                logger.info { "after" }
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
        shortName = "i",
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
            (barometerInterval * 1000).toLong(),
            getProperty("barometer.cmd")
        )
    }
}

// #####################################################################################################################
// #####################################################################################################################
// #####################################################################################################################
private suspend fun processBarometerObservation(observationJson: String, influxOperations: InfluxOperations) {
    logger.info { "Received: $observationJson" }
    val observation = deserializeIncomingBarometer(observationJson)
    val time = Instant.now().toEpochMilli()

    if (observation == null) {
        logger.info{"Received invalid observation data from barometer: $observationJson"}
        return
    }

    influxOperations.recordPressure(observation.pressureHpa,observation.sensorTempC, time)

}

// #####################################################################################################################
// #####################################################################################################################
// #####################################################################################################################
private suspend fun processObservation(observationJson: String, influxOperations: InfluxOperations) {
    logger.info{"Received: $observationJson"}
    val observation = deserializeIncomingObservation(observationJson)
    val time = Instant.now().toEpochMilli()

    if (observation == null) {
        logger.info{"Received invalid observation data: $observationJson"}
        return
    }

    //TODO: Need to filter out duplicate records (look at sequence number)?


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