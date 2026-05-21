package com.gyeonggisumgil.app

import com.gyeonggisumgil.app.data.tmap.TmapPedestrianRouteParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TmapPedestrianRouteParserTest {
    private val parser = TmapPedestrianRouteParser()

    @Test
    fun parseConvertsLineStringToRouteCandidate() {
        val route = parser.parse(
            responseJson = samplePedestrianRouteJson,
            routeId = "tmap-recommended",
            title = "큰 길 위주 경로",
            highlightLabel = "큰 길 위주",
            routeColorArgb = 0xFF2E7D5B,
            fallbackSummary = "Tmap 보행자 경로입니다."
        )

        assertEquals("tmap-recommended", route.id)
        assertEquals(1800, route.distanceMeters)
        assertEquals(24, route.durationMinutes)
        assertEquals("수원시청 을 따라 이동", route.exposureSummary)
        assertEquals(3, route.coordinates.size)
        assertEquals(37.2636, route.coordinates.first().latitude, 0.0001)
        assertEquals(127.0286, route.coordinates.first().longitude, 0.0001)
    }

    @Test
    fun parseKeepsDisplayMetadata() {
        val route = parser.parse(
            responseJson = samplePedestrianRouteJson,
            routeId = "tmap-avoid-stairs",
            title = "산책 경로",
            highlightLabel = "산책 우선",
            routeColorArgb = 0xFF356FB3,
            fallbackSummary = "Tmap 보행자 경로입니다."
        )

        assertEquals("산책 우선", route.highlightLabel)
        assertEquals(0xFF356FB3, route.routeColorArgb)
        assertTrue(route.coordinates.isNotEmpty())
    }

    private val samplePedestrianRouteJson = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": {
                "type": "Point",
                "coordinates": [127.0286, 37.2636]
              },
              "properties": {
                "totalDistance": 1800,
                "totalTime": 1410,
                "description": "수원시청 을 따라 이동"
              }
            },
            {
              "type": "Feature",
              "geometry": {
                "type": "LineString",
                "coordinates": [
                  [127.0286, 37.2636],
                  [127.0332, 37.2704],
                  [127.0471, 37.2794]
                ]
              },
              "properties": {
                "description": "광교호수공원 방향으로 이동"
              }
            }
          ]
        }
    """.trimIndent()
}
