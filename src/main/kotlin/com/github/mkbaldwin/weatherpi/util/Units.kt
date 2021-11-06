package com.github.mkbaldwin.weatherpi.util

import org.apache.commons.math3.util.Precision

fun celsiusToFahrenheit(c: Double): Double = Precision.round((c * 1.8) + 32, 1)

fun fahrenheitToCelsius(f: Double): Double = Precision.round((f - 32.0) / 1.8, 1)

fun kilometersToMiles(km: Double): Double = km * 0.621371

fun milesToKilometers(mi: Double): Double = mi * 1.609

fun inchesToMillimeters(inch: Double): Double = inch * 25.4

fun millimetersToInches(mm: Double): Double = mm / 25.4