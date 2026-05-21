package com.gyeonggisumgil.app

import com.gyeonggisumgil.app.domain.model.RouteCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteCandidateTest {
    @Test
    fun routeCandidateKeepsAirScore() {
        val route = RouteCandidate(
            id = "clean",
            title = "깨끗한 경로",
            distanceMeters = 1800,
            durationMinutes = 24,
            airScore = 86,
            exposureSummary = "공원 인접 경로",
            highlightLabel = "대기질 우선",
            routeColorArgb = 0xFF2E7D5B
        )

        assertEquals(86, route.airScore)
    }
}
