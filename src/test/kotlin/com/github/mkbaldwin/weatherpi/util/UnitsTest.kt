package com.github.mkbaldwin.weatherpi.util

import org.junit.jupiter.api.Test

class UnitsTest {

    @Test
    fun celsiusToFahrenheitTest() {
        val expected = 33.8
        val calculated = celsiusToFahrenheit(1.0)
        assert(calculated == expected) { "Conversion failed, expected value $expected, actual value $calculated" }
    }

    @Test
    fun fahrenheitToCelsiusTest() {
        val expected = 1.0
        val calculated = fahrenheitToCelsius(33.8)
        println(calculated)
        assert(calculated == expected) { "Conversion failed, expected value $expected, actual value $calculated" }
    }

    @Test
    fun kilometersToMilesTest() {
        val expected = 0.621371
        val calculated = kilometersToMiles(1.0)
        assert(calculated == expected) { "Conversion failed, expected value $expected, actual value $calculated" }
    }

    @Test
    fun milesToKilometersTest() {
        val expected = 1.609
        val calculated = milesToKilometers(1.0)
        assert(calculated == expected) { "Conversion failed, expected value $expected, actual value $calculated" }
    }

    @Test
    fun inchesToMillimetersTest() {
        val expected = 25.4
        val calculated = inchesToMillimeters(1.0)
        assert(calculated == expected) { "Conversion failed, expected value $expected, actual value $calculated" }
    }

    @Test
    fun millimetersToInches() {
        val expected = 1.0
        val calculated = millimetersToInches(25.4)
        assert(calculated == expected) { "Conversion failed, expected value $expected, actual value $calculated" }
    }
}