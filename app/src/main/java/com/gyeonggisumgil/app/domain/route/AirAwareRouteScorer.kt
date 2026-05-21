package com.gyeonggisumgil.app.domain.route

import com.gyeonggisumgil.app.domain.model.AirQualityReading
import com.gyeonggisumgil.app.domain.model.AirQualityStation
import com.gyeonggisumgil.app.domain.model.GeoPoint
import com.gyeonggisumgil.app.domain.model.RouteCandidate
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class AirAwareRouteScorer {
    fun rankRoutes(
        routes: List<RouteCandidate>,
        stations: List<AirQualityStation>,
        readings: List<AirQualityReading>
    ): List<RouteCandidate> {
        val readingByStation = readings.associateBy { it.stationId }

        return routes
            .map { route ->
                val routeReadings = route.coordinates
                    .mapNotNull { point ->
                        stations.nearestTo(point)?.let { station -> readingByStation[station.id] }
                    }
                    .distinctBy { it.stationId }

                val averagePm10 = routeReadings.mapNotNull { it.pm10 }.averageOrNull()
                val averagePm25 = routeReadings.mapNotNull { it.pm25 }.averageOrNull()
                val adjustedScore = route.airScore.adjustForAirQuality(averagePm10, averagePm25)

                route.copy(
                    airScore = adjustedScore,
                    exposureSummary = route.exposureSummary.withAirQualityEvidence(
                        averagePm10 = averagePm10,
                        averagePm25 = averagePm25
                    )
                )
            }
            .sortedByDescending { it.airScore }
    }

    private fun Int.adjustForAirQuality(
        averagePm10: Double?,
        averagePm25: Double?
    ): Int {
        if (averagePm10 == null && averagePm25 == null) {
            return this
        }

        val pm10Penalty = averagePm10?.let { ((it - 30.0) / 2.0).coerceAtLeast(0.0) } ?: 0.0
        val pm25Penalty = averagePm25?.let { ((it - 15.0) * 1.5).coerceAtLeast(0.0) } ?: 0.0

        return (this - pm10Penalty - pm25Penalty)
            .roundToInt()
            .coerceIn(0, 100)
    }

    private fun String.withAirQualityEvidence(
        averagePm10: Double?,
        averagePm25: Double?
    ): String {
        if (averagePm10 == null && averagePm25 == null) {
            return this
        }

        val pm10Text = averagePm10?.let { "PM10 ${it.roundToInt()}" } ?: "PM10 정보 없음"
        val pm25Text = averagePm25?.let { "PM2.5 ${it.roundToInt()}" } ?: "PM2.5 정보 없음"

        return "$this 주변 관측소 평균 $pm10Text, ${pm25Text}를 반영했습니다."
    }

    private fun List<Int>.averageOrNull(): Double? {
        return if (isEmpty()) null else average()
    }

    private fun List<AirQualityStation>.nearestTo(point: GeoPoint): AirQualityStation? {
        return minByOrNull { station ->
            point.approximateDistanceTo(
                GeoPoint(
                    latitude = station.latitude,
                    longitude = station.longitude
                )
            )
        }
    }

    private fun GeoPoint.approximateDistanceTo(other: GeoPoint): Double {
        val latitudeScale = 111_000.0
        val longitudeScale = latitudeScale * cos(Math.toRadians((latitude + other.latitude) / 2.0))
        val latitudeDistance = (latitude - other.latitude) * latitudeScale
        val longitudeDistance = (longitude - other.longitude) * longitudeScale

        return sqrt(latitudeDistance.pow(2) + longitudeDistance.pow(2))
    }
}
