package com.gyeonggisumgil.app.data.tmap

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gyeonggisumgil.app.domain.model.GeoPoint
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class TmapPlaceResolver(
    private val appKey: String,
    private val client: OkHttpClient = OkHttpClient()
) {
    fun resolve(query: String): TmapPlace? {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return null

        return if (normalizedQuery.looksLikeRoadAddress()) {
            resolveRoadAddress(normalizedQuery) ?: resolvePoi(normalizedQuery)
        } else {
            resolvePoi(normalizedQuery) ?: resolveRoadAddress(normalizedQuery)
        }
    }

    fun reverseGeocode(point: GeoPoint): String? {
        val url = "https://apis.openapi.sk.com/tmap/geo/reversegeocoding".toHttpUrl().newBuilder()
            .addQueryParameter("version", "1")
            .addQueryParameter("format", "json")
            .addQueryParameter("coordType", "WGS84GEO")
            .addQueryParameter("addressType", "A10")
            .addQueryParameter("lon", point.longitude.toString())
            .addQueryParameter("lat", point.latitude.toString())
            .build()

        val addressInfo = getJson(url.toString()).getAsJsonObject("addressInfo") ?: return null
        val roadAddress = listOfNotNull(
            addressInfo.stringOrNull("city_do"),
            addressInfo.stringOrNull("gu_gun"),
            addressInfo.stringOrNull("roadName"),
            addressInfo.stringOrNull("buildingIndex")
        ).joinToString(" ").takeIf { it.isNotBlank() }
        val buildingName = addressInfo.stringOrNull("buildingName")
        val lotAddress = listOfNotNull(
            addressInfo.stringOrNull("city_do"),
            addressInfo.stringOrNull("gu_gun"),
            addressInfo.stringOrNull("legalDong"),
            addressInfo.stringOrNull("bunji")
        ).joinToString(" ").takeIf { it.isNotBlank() }

        return listOfNotNull(roadAddress, buildingName).joinToString(" ").takeIf { it.isNotBlank() }
            ?: lotAddress
            ?: addressInfo.stringOrNull("fullAddress")
    }

    private fun resolveRoadAddress(query: String): TmapPlace? {
        val url = "https://apis.openapi.sk.com/tmap/geo/fullAddrGeo".toHttpUrl().newBuilder()
            .addQueryParameter("version", "1")
            .addQueryParameter("format", "json")
            .addQueryParameter("coordType", "WGS84GEO")
            .addQueryParameter("addressFlag", "F02")
            .addQueryParameter("fullAddr", query)
            .build()

        val root = getJson(url.toString())
        val coordinates = root
            .getAsJsonObject("coordinateInfo")
            ?.getAsJsonArray("coordinate")
            ?: return null

        return coordinates.firstObjectOrNull()?.let { coordinate ->
            val longitude = coordinate.stringOrNull("newLonEntr")
                ?: coordinate.stringOrNull("newLon")
                ?: coordinate.stringOrNull("lonEntr")
                ?: coordinate.stringOrNull("lon")
            val latitude = coordinate.stringOrNull("newLatEntr")
                ?: coordinate.stringOrNull("newLat")
                ?: coordinate.stringOrNull("latEntr")
                ?: coordinate.stringOrNull("lat")

            if (longitude == null || latitude == null) {
                null
            } else {
                TmapPlace(
                    name = coordinate.stringOrNull("newBuildingName")
                        ?: coordinate.stringOrNull("buildingName")
                        ?: query,
                    longitude = longitude.toDouble(),
                    latitude = latitude.toDouble()
                )
            }
        }
    }

    private fun resolvePoi(query: String): TmapPlace? {
        val url = "https://apis.openapi.sk.com/tmap/pois".toHttpUrl().newBuilder()
            .addQueryParameter("version", "1")
            .addQueryParameter("format", "json")
            .addQueryParameter("reqCoordType", "WGS84GEO")
            .addQueryParameter("resCoordType", "WGS84GEO")
            .addQueryParameter("searchKeyword", query)
            .build()

        val root = getJson(url.toString())
        val pois = root
            .getAsJsonObject("searchPoiInfo")
            ?.getAsJsonObject("pois")
            ?.getAsJsonArray("poi")
            ?: return null

        return pois.firstObjectOrNull()?.let { poi ->
            val longitude = poi.stringOrNull("frontLon") ?: poi.stringOrNull("noorLon")
            val latitude = poi.stringOrNull("frontLat") ?: poi.stringOrNull("noorLat")

            if (longitude == null || latitude == null) {
                null
            } else {
                TmapPlace(
                    name = poi.stringOrNull("name") ?: query,
                    longitude = longitude.toDouble(),
                    latitude = latitude.toDouble()
                )
            }
        }
    }

    private fun getJson(url: String): JsonObject {
        val request = Request.Builder()
            .url(url)
            .addHeader("appKey", appKey)
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("Tmap place resolve failed: ${response.code} $body")
            }
            return JsonParser.parseString(body).asJsonObject
        }
    }

    private fun String.looksLikeRoadAddress(): Boolean {
        return ROAD_ADDRESS_PATTERN.containsMatchIn(this)
    }

    private fun Iterable<com.google.gson.JsonElement>.firstObjectOrNull(): JsonObject? {
        return firstOrNull { it.isJsonObject }?.asJsonObject
    }

    private fun JsonObject.stringOrNull(name: String): String? {
        val element = get(name) ?: return null
        if (!element.isJsonPrimitive || !element.asJsonPrimitive.isString) return null

        return element.asString.takeIf { it.isNotBlank() }
    }

    companion object {
        private val ROAD_ADDRESS_PATTERN = Regex(""".*(로|길|대로)\s*\d+.*""")
    }
}
