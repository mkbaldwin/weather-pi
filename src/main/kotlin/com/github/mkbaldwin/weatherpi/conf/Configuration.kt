package com.github.mkbaldwin.weatherpi.conf

data class Configuration(
    val influxHost: String,
    val influxBucket: String,
    val influxOrg: String,
    val influxToken: String
)