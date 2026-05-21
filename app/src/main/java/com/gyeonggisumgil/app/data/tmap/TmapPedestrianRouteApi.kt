package com.gyeonggisumgil.app.data.tmap

import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class TmapPedestrianRouteApi(
    private val appKey: String,
    private val client: OkHttpClient = OkHttpClient()
) {
    fun getPedestrianRoute(request: TmapPedestrianRouteRequest): String {
        require(appKey.isNotBlank()) { "TMAP_APP_KEY is blank." }

        val bodyJson = JsonObject().apply {
            addProperty("startX", request.start.longitude.toString())
            addProperty("startY", request.start.latitude.toString())
            addProperty("endX", request.destination.longitude.toString())
            addProperty("endY", request.destination.latitude.toString())
            addProperty("startName", request.start.name)
            addProperty("endName", request.destination.name)
            addProperty("reqCoordType", "WGS84GEO")
            addProperty("resCoordType", "WGS84GEO")
            addProperty("searchOption", request.searchOption.value)
        }.toString()

        val httpRequest = Request.Builder()
            .url(TMAP_PEDESTRIAN_ROUTE_URL)
            .addHeader("appKey", appKey)
            .addHeader("Accept", "application/json")
            .post(bodyJson.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("Tmap pedestrian route failed: ${response.code} $responseBody")
            }
            return responseBody
        }
    }

    companion object {
        private const val TMAP_PEDESTRIAN_ROUTE_URL =
            "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1&format=json"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
