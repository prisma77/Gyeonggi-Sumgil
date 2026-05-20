package com.gyeonggisumgil.app.data

import com.gyeonggisumgil.app.domain.model.RouteCandidate

interface RouteRepository {
    fun getRecommendedRoutes(
        start: String,
        destination: String
    ): List<RouteCandidate>
}
