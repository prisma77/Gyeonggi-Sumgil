package com.gyeonggisumgil.app.domain.model

data class AirQualityReading(
    val stationId: String,
    val measuredAt: String,
    val pm10: Int?,
    val pm25: Int?,
    val grade: AirQualityGrade
)

enum class AirQualityGrade {
    GOOD,
    NORMAL,
    BAD,
    VERY_BAD,
    UNKNOWN
}
