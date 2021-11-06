package com.github.mkbaldwin.weatherpi.json

import org.junit.jupiter.api.Test

class IncomingObservationTest {
    @Test
    fun imperialUnitsTest() {
        val testData = loadTestData("sample-imperial.json")
        val obj = deserializeIncomingObservation(testData)

        assert(obj.temperatureF == 55.6) { "Temperature F did not match." }
        assert(obj.windSpeedKmh == 0.0) { "Wind Speed did not match." }
        assert(obj.humidity == 90) { "Humidity did not match." }
        assert(obj.sequenceNum == 2) { "Sequence Number did not match." }
    }

    @Test
    fun metricUnitsTest() {
        val testData = loadTestData("sample-metric.json")
        val obj = deserializeIncomingObservation(testData)

        assert(obj.temperatureC == 8.0) { "Temperature F did not match." }
        assert(obj.windSpeedKmh == 14.245) { "Wind Speed did not match." }
        assert(obj.humidity == 49) { "Humidity did not match." }
        assert(obj.sequenceNum == 0) { "Sequence Number did not match." }
    }

    @Test
    fun rainImperialUnitsTest() {
        val testData = loadTestData("sample-rain-imperial.json")
        val obj = deserializeIncomingObservation(testData)

        assert(obj.rainIn == 59.580) { "Rain mm did not match." }
        assert(obj.windSpeedKmh == 0.0) { "Wind Speed did not match." }
        assert(obj.windDirection == 67.5) { "Wind Direction did not match." }
        assert(obj.sequenceNum == 1) { "Sequence Number did not match." }
    }

    @Test
    fun rainMetricUnitsTest() {
        val testData = loadTestData("sample-rain-metric.json")
        val obj = deserializeIncomingObservation(testData)

        assert(obj.rainMm == 3346.202) { "Rain mm did not match." }
        assert(obj.windSpeedKmh == 11.761) { "Wind Speed did not match." }
        assert(obj.windDirection == 315.0) { "Wind Direction did not match." }
        assert(obj.sequenceNum == 0) { "Sequence Number did not match." }
    }

    private fun loadTestData(fileName: String): String = this.javaClass.getResource(fileName)?.readText() ?: ""
}