package com.github.mkbaldwin.weatherpi.persistence

import com.github.mkbaldwin.weatherpi.util.celsiusToFahrenheit
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.influxdb.client.kotlin.QueryKotlinApi
import com.influxdb.client.write.Point
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first

//TODO: Pull these from external configuration
private const val influxToken =
    "5evDxwgsVR610NBJOiGZux05J1Z7-Ilem9k5s3T_ufsZ7DbUYVn8lEXIRoWP8jttApfi5MYUknH9rWXUNSvFsg=="
private const val influxOrg = "dev"
private const val influxBucket = "weatherpi"
private const val influxHost = "http://localhost:8086"

// Create the connection to influx for all the queries to use.
private val client = InfluxDBClientKotlinFactory.create(influxHost, influxToken.toCharArray(), influxOrg, influxBucket)
private val writeApi = client.getWriteKotlinApi();
private val queryClient = client.getQueryKotlinApi()

// #####################################################################################################################
suspend fun recordTemperature(tempC: Double, timestamp: Long) = with(Point.measurement("Temperature")) {
    addField("value_c", tempC)
    addField("value_f", celsiusToFahrenheit(tempC))
    time(timestamp, WritePrecision.MS)
    writeApi.writePoint(this)
}

// #####################################################################################################################
suspend fun recordWind(windSpeedKmh: Double, windDirection: Double?, timestamp: Long) =
    with(Point.measurement("Wind")) {
        addField("speed_kmh", windSpeedKmh)
        time(timestamp, WritePrecision.MS)
        windDirection?.let { addField("direction", it) }
        writeApi.writePoint(this)
    }

// #####################################################################################################################
suspend fun recordHumidity(humidity: Int, timestamp: Long) = with(Point.measurement("Humidity")) {
    addField("value", humidity)
    time(timestamp, WritePrecision.MS)
    writeApi.writePoint(this)
}

// #####################################################################################################################
suspend fun recordRain(rainMm: Double, timestamp: Long) {
    // Rain measurements are different from others from this weather station. The weather station reports
    // the cumulative rain total in mm. So, we need to get the last measurement (if one exists) and use it
    // to calculate how much rain has fallen since the previous observation.
    val previousRainTotal = queryLatestRainTotal()

    println("previousRainTotal: $previousRainTotal")

    with(Point.measurement("Rain")) {
        addField("cumulative_mm", rainMm)
        time(timestamp, WritePrecision.MS)

        // Now we want to calculate the rain accumulated since the last measurement (if there was a last measurement).
        // TODO: This code assumes the value will NOT rollover back to zero. It could... so this needs to be handled
        // TODO: in the future. Not sure what the maximum observation is from the gauge.
        previousRainTotal?.let { prevRainTotal ->
            val rainInterval = rainMm - prevRainTotal
            addField("interval_mm", rainInterval)
        }

        writeApi.writePoint(this)
    }
}

// #####################################################################################################################
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun queryLatestRainTotal(): Double? {
    val query = """
        from(bucket: "$influxBucket")
          |> range(start: -1w)
          |> filter(fn: (r) => r["_measurement"] == "Rain")
          |> filter(fn: (r) => r["_field"] == "cumulative_mm")
          |> last()
          |> yield()
    """.trimIndent()

    val results = queryClient.query(query)

//    if (results.isEmpty) {
//        return null
//    }

    return results.consumeAsFlow().first().value as Double
}
