package com.gyeonggisumgil.app.domain.model

data class AirQualityStation(
    val id: String,
    val name: String,
    val city: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)
