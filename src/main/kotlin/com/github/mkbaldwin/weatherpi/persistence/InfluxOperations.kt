package com.github.mkbaldwin.weatherpi.persistence

import com.github.mkbaldwin.weatherpi.conf.Configuration
import com.github.mkbaldwin.weatherpi.util.*
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.kotlin.InfluxDBClientKotlin
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.influxdb.client.kotlin.QueryKotlinApi
import com.influxdb.client.kotlin.WriteKotlinApi
import com.influxdb.client.write.Point
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}

/**
 * Provides operations for adding, querying data in the influx database.
 */
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

    /**
     * Record a temperature observation. Accepts values in either C or F and performs conversions as required.
     *
     * @param tempC Wind speed observation in F, or null.
     * @param tempF Wind speed observation in C, or null.
     * @param timestamp Timestamp of the observation.
     */
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

    /**
     * Record a wind speed observation. Accepts values in either km/h or mi/h and performs conversions as required.
     *
     * @param windSpeedKmh Wind speed observation in km/h, or null.
     * @param windSpeedMph Wind speed observation in mi/h, or null.
     * @param windDirection Wind direction observation in degrees, or null.
     * @param timestamp Timestamp of the observation.
     */
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

    /**
     * Record a humidity (%) observation. Currently, these are tracked as integers since the weather station reports
     * that way.
     *
     * @param humidity Current % humidity observation.
     * @param timestamp Timestamp of the observation.
     */
    suspend fun recordHumidity(humidity: Int, timestamp: Long) = with(Point.measurement("Humidity")) {
        addField("value", humidity)
        time(timestamp, WritePrecision.MS)
        writeApi.writePoint(this)
    }

    /**
     * Record a rainfall observation.
     *
     * Rain measurements are different from others from this weather station. The weather station reports the
     * cumulative rain total in mm (or maybe in depending on mode). So, we need to get the last measurement
     * (if one exists) and use it to calculate how much rain has fallen since the previous observation. The cumulative
     * totals will all be tracked in mm and conversions done as required.
     *
     * @param rainMm Current rain observation in mm, or null.
     * @param rainIn Current rain observation in inches, or null.
     * @param timestamp Timestamp of the observation.
     */
    suspend fun recordRain(rainMm: Double?, rainIn: Double?, timestamp: Long) {

        val previousRainTotal = queryLatestRainTotal()

        logger.debug { "previousRainTotal: $previousRainTotal" }

        val currentRainMm = when {
            rainMm != null -> rainMm
            rainIn != null -> inchesToMillimeters(rainIn)
            // This should never be possible... but if both are null just assume there was no rain measurement.
            else -> 0.0
        }

        with(Point.measurement("Rain")) {
            addField("cumulative_mm", currentRainMm)
            time(timestamp, WritePrecision.MS)

            // Now we want to calculate the rain accumulated since the last measurement (if there was a last measurement).
            // TODO: This code assumes the value will NOT rollover back to zero. It could... so this needs to be handled
            // TODO: in the future. Not sure what the maximum observation is from the gauge.
            previousRainTotal.let { prevRainTotal ->
                val rainIntervalMm = currentRainMm - prevRainTotal
                addField("interval_mm", rainIntervalMm)
                addField("interval_in", millimetersToInches(rainIntervalMm))
            }

            writeApi.writePoint(this)
        }
    }

    /**
     * Locate the latest rain total measurement in the database.
     */
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
