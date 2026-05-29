package com.gyeonggisumgil.app.data.tmap

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gyeonggisumgil.app.domain.model.GeoPoint
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class TmapPlaceResolver(
    private val appKey: String,
    private val client: OkHttpClient = defaultTmapPlaceClient()
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
        return query.poiSearchVariants()
            .firstNotNullOfOrNull { searchKeyword ->
                resolvePoiByKeyword(
                    originalQuery = query,
                    searchKeyword = searchKeyword
                )
            }
    }

    private fun resolvePoiByKeyword(
        originalQuery: String,
        searchKeyword: String
    ): TmapPlace? {
        val url = "https://apis.openapi.sk.com/tmap/pois".toHttpUrl().newBuilder()
            .addQueryParameter("version", "1")
            .addQueryParameter("format", "json")
            .addQueryParameter("reqCoordType", "WGS84GEO")
            .addQueryParameter("resCoordType", "WGS84GEO")
            .addQueryParameter("searchKeyword", searchKeyword)
            .build()

        val root = getJson(url.toString())
        val pois = root
            .getAsJsonObject("searchPoiInfo")
            ?.getAsJsonObject("pois")
            ?.getAsJsonArray("poi")
            ?: return null

        return pois
            .mapNotNull { element ->
                val poi = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                val longitude = poi.stringOrNull("frontLon") ?: poi.stringOrNull("noorLon")
                val latitude = poi.stringOrNull("frontLat") ?: poi.stringOrNull("noorLat")
                val name = poi.stringOrNull("name") ?: return@mapNotNull null

                if (longitude == null || latitude == null) {
                    null
                } else {
                    PoiCandidate(
                        place = TmapPlace(
                            name = name,
                            longitude = longitude.toDouble(),
                            latitude = latitude.toDouble()
                        ),
                        score = name.scoreForPlaceQuery(originalQuery)
                    )
                }
            }
            .filter { candidate ->
                !originalQuery.isNaturalPlaceQuery() ||
                    !candidate.place.name.isSecondaryPoiName() ||
                    candidate.score >= NATURAL_PLACE_SECONDARY_POI_SCORE_LIMIT
            }
            .maxByOrNull { it.score }
            ?.place
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
        private const val NATURAL_PLACE_SECONDARY_POI_SCORE_LIMIT = 25
    }
}

private data class PoiCandidate(
    val place: TmapPlace,
    val score: Int
)

private fun String.poiSearchVariants(): List<String> {
    val query = trim()
    if (!query.isNaturalPlaceQuery()) return listOf(query)

    return listOf(
        query,
        "$query 공원",
        "$query 산책로",
        "$query 둘레길",
        "$query 수변공원"
    ).distinct()
}

private fun String.scoreForPlaceQuery(query: String): Int {
    val name = normalizedPlaceText()
    val normalizedQuery = query.normalizedPlaceText()
    var score = 0

    if (name == normalizedQuery) score += 160
    if (name.contains(normalizedQuery)) score += 95
    if (normalizedQuery.contains(name) && name.length >= 3) score += 45

    NATURAL_PLACE_KEYWORDS.forEach { keyword ->
        if (name.contains(keyword)) score += 18
        if (normalizedQuery.contains(keyword) && name.contains(keyword)) score += 18
    }

    if (query.isNaturalPlaceQuery() && isSecondaryPoiName()) score -= 120
    if (query.isNaturalPlaceQuery() && !NATURAL_PLACE_KEYWORDS.any { name.contains(it) }) score -= 30

    return score
}

private fun String.isNaturalPlaceQuery(): Boolean {
    val text = normalizedPlaceText()
    val asksSecondaryPoi = SECONDARY_POI_KEYWORDS.any { text.contains(it) }
    return !asksSecondaryPoi && NATURAL_PLACE_KEYWORDS.any { text.contains(it) }
}

private fun String.isSecondaryPoiName(): Boolean {
    val text = normalizedPlaceText()
    return SECONDARY_POI_KEYWORDS.any { text.contains(it) }
}

private fun String.normalizedPlaceText(): String {
    return lowercase()
        .replace(" ", "")
        .replace("-", "")
        .replace("_", "")
}

private val NATURAL_PLACE_KEYWORDS = listOf(
    "호수",
    "공원",
    "저수지",
    "하천",
    "천",
    "강변",
    "수변",
    "산책로",
    "둘레길",
    "습지"
)

private val SECONDARY_POI_KEYWORDS = listOf(
    "주차",
    "주차장",
    "공영주차장",
    "역",
    "카페",
    "커피",
    "식당",
    "음식점",
    "매장",
    "마트",
    "편의점",
    "아파트",
    "오피스텔",
    "상가",
    "병원",
    "학교",
    "주유소",
    "화장실"
)

private fun defaultTmapPlaceClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(4, TimeUnit.SECONDS)
        .build()
}
