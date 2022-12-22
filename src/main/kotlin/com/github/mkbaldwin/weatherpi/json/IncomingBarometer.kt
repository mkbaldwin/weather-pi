package com.github.mkbaldwin.weatherpi.json

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParseException
import com.github.mkbaldwin.weatherpi.util.deserialize

// Temperature is inside
//{ temperature_c="22.8505859375", pressure_hpa="984.6174487757261" }
@JsonIgnoreProperties(ignoreUnknown = true)
data class IncomingBarometer(
    @get:JsonProperty("temperature_c")
    val sensorTempC: Double,

    @get:JsonProperty("pressure_hpa")
    val pressureHpa: Double
)


fun deserializeIncomingBarometer(json: String): IncomingBarometer?{
    try {
        return deserialize<IncomingBarometer>(json)
    }
    catch (ex: JsonParseException) {
        ex.printStackTrace()
        return null
    }
}
