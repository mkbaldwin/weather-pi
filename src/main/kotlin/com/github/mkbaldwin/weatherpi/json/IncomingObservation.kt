package com.github.mkbaldwin.weatherpi.json

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.mkbaldwin.weatherpi.util.deserialize
import java.text.SimpleDateFormat
import java.time.Instant


private val incomingDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

@JsonIgnoreProperties(ignoreUnknown = true)
data class IncomingObservation(
    @get:JsonProperty("time")
    val time: String,

    @get:JsonProperty("wind_avg_km_h")
    val windSpeed: Double? = null,

    @get:JsonProperty("wind_dir_deg")
    val windDirection: Double? = null,

    @get:JsonProperty("temperature_C")
    val temperature: Double? = null,

    @get:JsonProperty("humidity")
    val humidity: Int? = null,

    @get:JsonProperty("rain_mm")
    val rain: Double? = null
) {
    fun getTimeInstant(): Instant = parseToInstant(time)

    private fun parseToInstant(time: String): Instant {
        val timePart = time.split(".")[0]
        return incomingDateFormat.parse(timePart).toInstant()
    }
}


fun deserializeIncomingObservation(json: String) = deserialize<IncomingObservation>(json)