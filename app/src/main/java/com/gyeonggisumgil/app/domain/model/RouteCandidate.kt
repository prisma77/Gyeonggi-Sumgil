package com.gyeonggisumgil.app.domain.model

data class RouteCandidate(
    val id: String,
    val title: String,
    val distanceMeters: Int,
    val durationMinutes: Int,
    val airScore: Int,
    val exposureSummary: String
)
