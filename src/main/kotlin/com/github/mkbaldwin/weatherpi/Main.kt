package com.github.mkbaldwin.weatherpi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.mkbaldwin.weatherpi.json.IncomingObservation
import com.github.mkbaldwin.weatherpi.persistence.recordHumidity
import com.github.mkbaldwin.weatherpi.persistence.recordRain
import com.github.mkbaldwin.weatherpi.persistence.recordTemperature
import com.github.mkbaldwin.weatherpi.persistence.recordWind
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.influxdb.client.kotlin.QueryKotlinApi
import com.influxdb.client.write.Point
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.first
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.time.Instant
import kotlin.math.max
import kotlin.random.Random




fun main() {

//    val testData = ClassLoader.getSystemClassLoader().getResource("test.data/data-input.json")?.readText()
//    testData?.split("\n")?.map { it.trim() }?.filter { it.isNotEmpty() }?.forEach { line ->
//        val observation = deserialize(line)
////        val time = observation.getTimeInstant().toEpochMilli()
//        val time = Instant.now().toEpochMilli()
//        println(observation)
//
//        runBlocking {
//            observation.temperature?.let {
//                recordTemperature(it, time)
//            }
//
//            observation.windSpeed?.let { it ->
//                recordWind(it, observation.windDirection, time)
//            }
//
//            observation.humidity?.let {
//                recordHumidity(it, time)
//            }
//
//            observation.rain?.let { it ->
//                recordRain(it, time)
//            }
//        }
//
//        Thread.sleep(30000L)
//    }

    var rainmm = 0.0
    for (i in 1..50) {
        val time = Instant.now().toEpochMilli()
        val newRain = 0.0 + Random.nextInt(0, 10)
        rainmm += newRain
        runBlocking {
            recordRain(rainmm, time)
            println("$time  ---  $rainmm  --- $newRain")
        }

        Thread.sleep(100L)
    }
}


