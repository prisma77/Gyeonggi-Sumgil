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
            exposureSummary = "공원 인접 경로"
        )

        assertEquals(86, route.airScore)
    }
}
