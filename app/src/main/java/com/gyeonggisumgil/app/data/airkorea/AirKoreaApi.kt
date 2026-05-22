package com.gyeonggisumgil.app.data.airkorea

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class AirKoreaApi(
    private val serviceKey: String,
    private val client: OkHttpClient = defaultClient()
) {
    fun getMeasurements(sidoName: String): List<AirKoreaMeasurement> {
        require(serviceKey.isNotBlank()) { "AIRKOREA_SERVICE_KEY is blank." }
        require(sidoName.isNotBlank()) { "sidoName is blank." }

        val url = AIR_KOREA_REALTIME_SIDO_URL.toHttpUrl().newBuilder()
            .addQueryParameter("serviceKey", serviceKey)
            .addQueryParameter("returnType", "json")
            .addQueryParameter("numOfRows", "200")
            .addQueryParameter("pageNo", "1")
            .addQueryParameter("sidoName", sidoName)
            .addQueryParameter("ver", "1.0")
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("AirKorea realtime sido request failed: ${response.code} $body")
            }

            val root = JsonParser.parseString(body).asJsonObject
            val header = root.getAsJsonObject("response")
                ?.getAsJsonObject("header")
            val resultCode = header?.stringOrNull("resultCode")
            if (resultCode != null && resultCode != "00") {
                error("AirKorea request failed: $resultCode ${header.stringOrNull("resultMsg").orEmpty()}")
            }

            val items = root.getAsJsonObject("response")
                ?.getAsJsonObject("body")
                ?.getAsJsonArray("items")
                ?: return emptyList()

            return items
                .filter { it.isJsonObject }
                .mapNotNull { item ->
                    val obj = item.asJsonObject
                    val stationName = obj.stringOrNull("stationName") ?: return@mapNotNull null
                    AirKoreaMeasurement(
                        stationName = stationName,
                        dataTime = obj.stringOrNull("dataTime").orEmpty(),
                        pm10Value = obj.intOrNull("pm10Value"),
                        pm25Value = obj.intOrNull("pm25Value"),
                        khaiGrade = obj.intOrNull("khaiGrade"),
                        pm10Grade = obj.intOrNull("pm10Grade"),
                        pm25Grade = obj.intOrNull("pm25Grade")
                    )
                }
        }
    }

    private fun JsonObject.stringOrNull(name: String): String? {
        val element = get(name) ?: return null
        if (!element.isJsonPrimitive) return null
        return element.asString.takeIf { it.isNotBlank() && it != "-" }
    }

    private fun JsonObject.intOrNull(name: String): Int? {
        return stringOrNull(name)?.toIntOrNull()
    }

    companion object {
        private const val AIR_KOREA_REALTIME_SIDO_URL =
            "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty"

        private fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .callTimeout(4, TimeUnit.SECONDS)
                .build()
        }
    }
}
