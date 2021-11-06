package com.github.mkbaldwin.weatherpi.persistence

import com.github.mkbaldwin.weatherpi.conf.Configuration
import com.github.mkbaldwin.weatherpi.util.celsiusToFahrenheit
import com.github.mkbaldwin.weatherpi.util.fahrenheitToCelsius
import com.github.mkbaldwin.weatherpi.util.kilometersToMiles
import com.github.mkbaldwin.weatherpi.util.milesToKilometers
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.kotlin.InfluxDBClientKotlin
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.influxdb.client.kotlin.QueryKotlinApi
import com.influxdb.client.kotlin.WriteKotlinApi
import com.influxdb.client.write.Point
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first

class InfluxOperations(configuration: Configuration) {
    // Create the connection to influx for all the queries to use.
    private val client: InfluxDBClientKotlin
    private val writeApi: WriteKotlinApi
    private val queryClient: QueryKotlinApi
    private val influxBucket: String

    // #################################################################################################################
    init {
        client = InfluxDBClientKotlinFactory.create(
            configuration.influxHost,
            configuration.influxToken.toCharArray(),
            configuration.influxOrg,
            configuration.influxBucket
        )

        influxBucket = configuration.influxBucket
        writeApi = client.getWriteKotlinApi()
        queryClient = client.getQueryKotlinApi()
    }

    // #################################################################################################################
    suspend fun recordTemperature(tempC: Double?, tempF: Double?, timestamp: Long) =
        with(Point.measurement("Temperature")) {
            var persist = false
            if (tempF != null) {
                addField("value_c", fahrenheitToCelsius(tempF))
                addField("value_f", tempF)
                persist = true
            } else if (tempC != null) {
                addField("value_c", tempC)
                addField("value_f", celsiusToFahrenheit(tempC))
                persist = true
            }

            if (persist) {
                time(timestamp, WritePrecision.MS)
                writeApi.writePoint(this)
            }
        }

    // #################################################################################################################
// TODO: What if this is mi_h instead of km_h
    suspend fun recordWind(windSpeedKmh: Double?, windSpeedMph: Double?, windDirection: Double?, timestamp: Long) =
        with(Point.measurement("Wind")) {
            var persist = false
            if (windSpeedKmh != null) {
                addField("speed_kmh", windSpeedKmh)
                addField("speed_mph", kilometersToMiles(windSpeedKmh))
                persist = true
            } else if (windSpeedMph != null) {
                addField("speed_kmh", milesToKilometers(windSpeedMph))
                addField("speed_mph", windSpeedMph)
                persist = true
            }

            if (persist) {
                time(timestamp, WritePrecision.MS)
                windDirection?.let { addField("direction", it) }
                writeApi.writePoint(this)
            }

        }

    // #################################################################################################################
    suspend fun recordHumidity(humidity: Int, timestamp: Long) = with(Point.measurement("Humidity")) {
        addField("value", humidity)
        time(timestamp, WritePrecision.MS)
        writeApi.writePoint(this)
    }

    // #################################################################################################################
    suspend fun recordRain(rainMm: Double, timestamp: Long) {
        // Rain measurements are different from others from this weather station. The weather station reports
        // the cumulative rain total in mm. So, we need to get the last measurement (if one exists) and use it
        // to calculate how much rain has fallen since the previous observation.
        val previousRainTotal = queryLatestRainTotal()

        println("previousRainTotal: $previousRainTotal")

        //TODO: What if this is rain_in instead of rain_mm
        with(Point.measurement("Rain")) {
            addField("cumulative_mm", rainMm)
            time(timestamp, WritePrecision.MS)

            // Now we want to calculate the rain accumulated since the last measurement (if there was a last measurement).
            // TODO: This code assumes the value will NOT rollover back to zero. It could... so this needs to be handled
            // TODO: in the future. Not sure what the maximum observation is from the gauge.
            previousRainTotal.let { prevRainTotal ->
                val rainInterval = rainMm - prevRainTotal
                addField("interval_mm", rainInterval)
            }

            writeApi.writePoint(this)
        }
    }

    // #################################################################################################################
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun queryLatestRainTotal(): Double {
        val query = """
        from(bucket: "${influxBucket}")
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
}
