package com.gyeonggisumgil.app

import com.gyeonggisumgil.app.data.SampleRouteRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleRouteRepositoryTest {
    private val repository = SampleRouteRepository()

    @Test
    fun recommendedRoutesAreSortedByAirScore() {
        val routes = repository.getRecommendedRoutes("수원시청", "광교호수공원")

        assertEquals("clean", routes.first().id)
        assertTrue(routes.first().airScore > routes.last().airScore)
    }

    @Test
    fun recommendedRoutesContainMapCoordinates() {
        val routes = repository.getRecommendedRoutes("수원시청", "광교호수공원")

        assertTrue(routes.all { it.coordinates.size >= 2 })
    }

    @Test
    fun recommendedRoutesContainDisplayMetadata() {
        val routes = repository.getRecommendedRoutes("수원시청", "광교호수공원")

        assertTrue(routes.all { it.highlightLabel.isNotBlank() })
        assertTrue(routes.all { it.routeColorArgb != 0L })
    }
}
