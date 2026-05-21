package com.gyeonggisumgil.app.data.tmap

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gyeonggisumgil.app.domain.model.GeoPoint
import com.gyeonggisumgil.app.domain.model.RouteCandidate

class TmapPedestrianRouteParser {
    fun parse(
        responseJson: String,
        routeId: String,
        title: String,
        highlightLabel: String,
        routeColorArgb: Long,
        fallbackSummary: String,
        baseAirScore: Int = 80
    ): RouteCandidate {
        val root = JsonParser.parseString(responseJson).asJsonObject
        val features = root.getAsJsonArray("features")

        var totalDistanceMeters = 0
        var totalTimeSeconds = 0
        val descriptions = mutableListOf<String>()
        val coordinates = mutableListOf<GeoPoint>()

        features.forEachObject { feature ->
            val properties = feature.getAsJsonObject("properties")
            totalDistanceMeters = properties.intOrFallback("totalDistance", totalDistanceMeters)
            totalTimeSeconds = properties.intOrFallback("totalTime", totalTimeSeconds)
            properties.stringOrNull("description")?.let(descriptions::add)

            val geometry = feature.getAsJsonObject("geometry")
            if (geometry.stringOrNull("type") == "LineString") {
                geometry.getAsJsonArray("coordinates").forEachCoordinate { longitude, latitude ->
                    coordinates.add(GeoPoint(latitude, longitude))
                }
            }
        }

        return RouteCandidate(
            id = routeId,
            title = title,
            distanceMeters = totalDistanceMeters,
            durationMinutes = secondsToMinutes(totalTimeSeconds),
            airScore = baseAirScore,
            exposureSummary = descriptions.firstOrNull() ?: fallbackSummary,
            highlightLabel = highlightLabel,
            routeColorArgb = routeColorArgb,
            coordinates = coordinates
        )
    }

    private fun secondsToMinutes(seconds: Int): Int {
        if (seconds <= 0) return 0
        return (seconds + 59) / 60
    }

    private fun JsonArray.forEachObject(block: (JsonObject) -> Unit) {
        forEach { element ->
            if (element.isJsonObject) {
                block(element.asJsonObject)
            }
        }
    }

    private fun JsonArray.forEachCoordinate(block: (longitude: Double, latitude: Double) -> Unit) {
        forEach { element ->
            if (element.isJsonArray) {
                val pair = element.asJsonArray
                if (pair.size() >= 2) {
                    block(pair[0].asDouble, pair[1].asDouble)
                }
            }
        }
    }

    private fun JsonObject.intOrFallback(name: String, fallback: Int): Int {
        val element = get(name) ?: return fallback
        return if (element.isJsonPrimitive && element.asJsonPrimitive.isNumber) {
            element.asInt
        } else {
            fallback
        }
    }

    private fun JsonObject.stringOrNull(name: String): String? {
        val element = get(name) ?: return null
        return if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
            element.asString
        } else {
            null
        }
    }
}
