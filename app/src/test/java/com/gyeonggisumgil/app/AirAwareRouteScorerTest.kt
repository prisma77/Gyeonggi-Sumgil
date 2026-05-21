package com.gyeonggisumgil.app

import com.gyeonggisumgil.app.domain.model.AirQualityGrade
import com.gyeonggisumgil.app.domain.model.AirQualityReading
import com.gyeonggisumgil.app.domain.model.AirQualityStation
import com.gyeonggisumgil.app.domain.model.GeoPoint
import com.gyeonggisumgil.app.domain.model.RouteCandidate
import com.gyeonggisumgil.app.domain.route.AirAwareRouteScorer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AirAwareRouteScorerTest {
    private val scorer = AirAwareRouteScorer()

    @Test
    fun rankRoutesPenalizesRoutesNearPoorAirQualityStations() {
        val cleanRoute = route(
            id = "clean",
            airScore = 90,
            coordinates = listOf(GeoPoint(37.0, 127.0), GeoPoint(37.001, 127.001))
        )
        val pollutedRoute = route(
            id = "polluted",
            airScore = 90,
            coordinates = listOf(GeoPoint(37.1, 127.1), GeoPoint(37.101, 127.101))
        )

        val rankedRoutes = scorer.rankRoutes(
            routes = listOf(pollutedRoute, cleanRoute),
            stations = listOf(
                station(id = "clean-station", latitude = 37.0, longitude = 127.0),
                station(id = "polluted-station", latitude = 37.1, longitude = 127.1)
            ),
            readings = listOf(
                reading(stationId = "clean-station", pm10 = 28, pm25 = 12),
                reading(stationId = "polluted-station", pm10 = 70, pm25 = 38)
            )
        )

        assertEquals("clean", rankedRoutes.first().id)
        assertTrue(rankedRoutes.first().airScore > rankedRoutes.last().airScore)
    }

    @Test
    fun rankRoutesAddsAirQualityEvidenceToSummary() {
        val rankedRoutes = scorer.rankRoutes(
            routes = listOf(
                route(
                    id = "clean",
                    airScore = 90,
                    coordinates = listOf(GeoPoint(37.0, 127.0))
                )
            ),
            stations = listOf(station(id = "station", latitude = 37.0, longitude = 127.0)),
            readings = listOf(reading(stationId = "station", pm10 = 42, pm25 = 21))
        )

        assertTrue(rankedRoutes.first().exposureSummary.contains("PM10 42"))
        assertTrue(rankedRoutes.first().exposureSummary.contains("PM2.5 21"))
    }

    private fun route(
        id: String,
        airScore: Int,
        coordinates: List<GeoPoint>
    ): RouteCandidate {
        return RouteCandidate(
            id = id,
            title = id,
            distanceMeters = 1000,
            durationMinutes = 15,
            airScore = airScore,
            exposureSummary = "테스트 경로입니다.",
            highlightLabel = "테스트",
            routeColorArgb = 0xFF2E7D5B,
            coordinates = coordinates
        )
    }

    private fun station(
        id: String,
        latitude: Double,
        longitude: Double
    ): AirQualityStation {
        return AirQualityStation(
            id = id,
            name = id,
            city = "수원시",
            address = "경기도 수원시",
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun reading(
        stationId: String,
        pm10: Int,
        pm25: Int
    ): AirQualityReading {
        return AirQualityReading(
            stationId = stationId,
            measuredAt = "2026-05-21 15:00",
            pm10 = pm10,
            pm25 = pm25,
            grade = AirQualityGrade.NORMAL
        )
    }
}
