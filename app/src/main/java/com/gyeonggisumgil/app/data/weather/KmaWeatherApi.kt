package com.gyeonggisumgil.app.data.weather

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gyeonggisumgil.app.domain.model.GeoPoint
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.floor
import kotlin.math.pow

class KmaWeatherApi(
    private val serviceKey: String,
    private val client: OkHttpClient = defaultClient()
) {
    fun getCurrentWeather(point: GeoPoint): KmaCurrentWeather? {
        require(serviceKey.isNotBlank()) { "KMA_SERVICE_KEY is blank." }

        val grid = point.toKmaGrid()
        val baseTime = KmaBaseTime.ultraShortNow()
        val nowcastValues = requestItems(
            endpoint = KMA_ULTRA_SHORT_NOWCAST_URL,
            baseTime = baseTime,
            grid = grid,
            valueField = "obsrValue"
        )
        val forecastValues = runCatching {
            requestItems(
                endpoint = KMA_ULTRA_SHORT_FORECAST_URL,
                baseTime = baseTime,
                grid = grid,
                valueField = "fcstValue"
            )
        }.getOrDefault(emptyMap())

        return KmaCurrentWeather(
            temperatureCelsius = nowcastValues["T1H"]?.toDoubleOrNull(),
            precipitationMm = nowcastValues["RN1"]?.toDoubleOrNull(),
            humidityPercent = nowcastValues["REH"]?.toIntOrNull(),
            windSpeedMetersPerSecond = nowcastValues["WSD"]?.toDoubleOrNull(),
            precipitationType = (forecastValues["PTY"] ?: nowcastValues["PTY"])?.toPrecipitationType(),
            skyState = forecastValues["SKY"]?.toSkyState(),
            measuredAt = "${baseTime.date.formatKmaDate()} ${baseTime.time.formatKmaTime()}",
            gridX = grid.x,
            gridY = grid.y
        )
    }

    private fun requestItems(
        endpoint: String,
        baseTime: KmaBaseTime,
        grid: KmaGrid,
        valueField: String
    ): Map<String, String> {
        val url = endpoint.toHttpUrl().newBuilder()
            .addQueryParameter("serviceKey", serviceKey)
            .addQueryParameter("pageNo", "1")
            .addQueryParameter("numOfRows", "1000")
            .addQueryParameter("dataType", "JSON")
            .addQueryParameter("base_date", baseTime.date)
            .addQueryParameter("base_time", baseTime.time)
            .addQueryParameter("nx", grid.x.toString())
            .addQueryParameter("ny", grid.y.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("KMA weather request failed: ${response.code} $body")
            }

            val root = JsonParser.parseString(body).asJsonObject
            val header = root.getAsJsonObject("response")
                ?.getAsJsonObject("header")
            val resultCode = header?.stringOrNull("resultCode")
            if (resultCode != null && resultCode != "00") {
                error("KMA weather request failed: $resultCode ${header.stringOrNull("resultMsg").orEmpty()}")
            }

            val items = root.getAsJsonObject("response")
                ?.getAsJsonObject("body")
                ?.getAsJsonObject("items")
                ?.getAsJsonArray("item")
                ?: return emptyMap()
            return items
                .filter { it.isJsonObject }
                .mapNotNull { item ->
                    val obj = item.asJsonObject
                    val category = obj.stringOrNull("category") ?: return@mapNotNull null
                    val value = obj.stringOrNull(valueField) ?: return@mapNotNull null
                    category to value
                }
                .toMap()
        }
    }

    private fun JsonObject.stringOrNull(name: String): String? {
        val element = get(name) ?: return null
        if (!element.isJsonPrimitive) return null
        return element.asString.takeIf { it.isNotBlank() && it != "-" }
    }

    private data class KmaGrid(val x: Int, val y: Int)

    private data class KmaBaseTime(val date: String, val time: String) {
        companion object {
            fun ultraShortNow(): KmaBaseTime {
                val zone = ZoneId.of("Asia/Seoul")
                val now = LocalDateTime.now(zone)
                val base = if (now.minute < 45) now.minusHours(1) else now
                return KmaBaseTime(
                    date = base.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                    time = base.format(DateTimeFormatter.ofPattern("HH00"))
                )
            }
        }
    }

    private fun GeoPoint.toKmaGrid(): KmaGrid {
        val re = 6371.00877
        val grid = 5.0
        val slat1 = Math.toRadians(30.0)
        val slat2 = Math.toRadians(60.0)
        val olon = Math.toRadians(126.0)
        val olat = Math.toRadians(38.0)
        val xo = 43.0
        val yo = 136.0
        val reGrid = re / grid
        val sn = kotlin.math.ln(kotlin.math.cos(slat1) / kotlin.math.cos(slat2)) /
            kotlin.math.ln(kotlin.math.tan(Math.PI * 0.25 + slat2 * 0.5) / kotlin.math.tan(Math.PI * 0.25 + slat1 * 0.5))
        val sf = kotlin.math.tan(Math.PI * 0.25 + slat1 * 0.5).pow(sn) * kotlin.math.cos(slat1) / sn
        val ro = reGrid * sf / kotlin.math.tan(Math.PI * 0.25 + olat * 0.5).pow(sn)
        var ra = reGrid * sf / kotlin.math.tan(Math.PI * 0.25 + Math.toRadians(latitude) * 0.5).pow(sn)
        var theta = Math.toRadians(longitude) - olon
        if (theta > Math.PI) theta -= 2.0 * Math.PI
        if (theta < -Math.PI) theta += 2.0 * Math.PI
        theta *= sn

        return KmaGrid(
            x = floor(ra * kotlin.math.sin(theta) + xo + 0.5).toInt(),
            y = floor(ro - ra * kotlin.math.cos(theta) + yo + 0.5).toInt()
        )
    }

    private fun String.formatKmaDate(): String {
        return if (length == 8) "${substring(0, 4)}-${substring(4, 6)}-${substring(6, 8)}" else this
    }

    private fun String.formatKmaTime(): String {
        return if (length == 4) "${substring(0, 2)}:${substring(2, 4)}" else this
    }

    companion object {
        private const val KMA_ULTRA_SHORT_NOWCAST_URL =
            "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst"
        private const val KMA_ULTRA_SHORT_FORECAST_URL =
            "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst"

        private fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .callTimeout(4, TimeUnit.SECONDS)
                .build()
        }
    }
}

data class KmaCurrentWeather(
    val temperatureCelsius: Double?,
    val precipitationMm: Double?,
    val humidityPercent: Int?,
    val windSpeedMetersPerSecond: Double?,
    val precipitationType: KmaPrecipitationType?,
    val skyState: KmaSkyState?,
    val measuredAt: String,
    val gridX: Int,
    val gridY: Int
)

enum class KmaPrecipitationType(val label: String) {
    None("없음"),
    Rain("비"),
    RainSnow("비/눈"),
    Snow("눈"),
    Shower("소나기"),
    Raindrop("빗방울"),
    RaindropSnow("빗방울/눈날림"),
    SnowFlurry("눈날림")
}

enum class KmaSkyState(val label: String) {
    Clear("맑음"),
    PartlyCloudy("구름많음"),
    Cloudy("흐림")
}

private fun String.toPrecipitationType(): KmaPrecipitationType? {
    return when (toIntOrNull()) {
        0 -> KmaPrecipitationType.None
        1 -> KmaPrecipitationType.Rain
        2 -> KmaPrecipitationType.RainSnow
        3 -> KmaPrecipitationType.Snow
        4 -> KmaPrecipitationType.Shower
        5 -> KmaPrecipitationType.Raindrop
        6 -> KmaPrecipitationType.RaindropSnow
        7 -> KmaPrecipitationType.SnowFlurry
        else -> null
    }
}

private fun String.toSkyState(): KmaSkyState? {
    return when (toIntOrNull()) {
        1 -> KmaSkyState.Clear
        3 -> KmaSkyState.PartlyCloudy
        4 -> KmaSkyState.Cloudy
        else -> null
    }
}
