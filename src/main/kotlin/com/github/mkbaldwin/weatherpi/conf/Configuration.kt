package com.github.mkbaldwin.weatherpi.conf

/**
 * Represent the configuration details for the application.
 */
data class Configuration(
    val influxHost: String,
    val influxBucket: String,
    val influxOrg: String,
    val influxToken: String,
    val barometerEnable: Boolean,
    val barometerIntervalMs: Long
)