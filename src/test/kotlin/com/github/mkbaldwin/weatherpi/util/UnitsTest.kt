package com.github.mkbaldwin.weatherpi.util

import org.junit.jupiter.api.Test

class UnitsTest {

    @Test
    fun celsiusToFahrenheitTest() {
        val expected = 32.0
        val calculated = celsiusToFahrenheit(0.1)
        assert(calculated == expected) { "Conversion failed, expected value $expected, actual value $calculated" }
    }

    @Test
    fun kilometersToMilesTest() {
        val expected = 0.621371
        val calculated = kilometersToMiles(1.0)
        assert(calculated == expected) { "Conversion failed, expected value $expected, actual value $calculated" }
    }
}