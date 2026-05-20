package com.gyeonggisumgil.app.domain.model

import com.naver.maps.geometry.LatLng

data class RouteCandidate(
    val id: String,
    val title: String,
    val distanceMeters: Int,
    val durationMinutes: Int,
    val airScore: Int,
    val exposureSummary: String,
    val coordinates: List<LatLng> = emptyList()
)
