package com.gyeonggisumgil.app.data

import com.gyeonggisumgil.app.domain.model.RouteCandidate
import com.gyeonggisumgil.app.domain.model.GeoPoint
import com.gyeonggisumgil.app.domain.route.AirAwareRouteScorer

class SampleRouteRepository(
    private val routeScorer: AirAwareRouteScorer = AirAwareRouteScorer()
) : RouteRepository {
    override fun getRecommendedRoutes(
        start: String,
        destination: String
    ): List<RouteCandidate> {
        if (start == "미사역" && destination == "미사강변중앙로 90") {
            return listOf(
                RouteCandidate(
                    id = "hanam-preview",
                    title = "하남 도보 확인 경로",
                    distanceMeters = 1038,
                    durationMinutes = 14,
                    airScore = 82,
                    exposureSummary = "미사역에서 미사강변중앙로 90까지 도보 경로 확인용 프리뷰입니다. Tmap 호출 후 실제 보행자 경로선으로 교체됩니다.",
                    highlightLabel = "도보 확인",
                    routeColorArgb = 0xFF2E7D5B,
                    coordinates = hanamPreviewCoordinates
                )
            )
        }

        val sampleRoutes = listOf(
            RouteCandidate(
                id = "clean",
                title = "깨끗한 경로",
                distanceMeters = 1800,
                durationMinutes = 24,
                airScore = 86,
                exposureSummary = "대로변을 줄이고 공원·하천 인접 구간을 우선 통과하는 추천 경로입니다.",
                highlightLabel = "대기질 우선",
                routeColorArgb = 0xFF2E7D5B,
                coordinates = cleanRouteCoordinates
            ),
            RouteCandidate(
                id = "balanced",
                title = "균형 경로",
                distanceMeters = 1600,
                durationMinutes = 21,
                airScore = 78,
                exposureSummary = "소요시간과 대기질 노출을 함께 고려한 일상 이동용 경로입니다.",
                highlightLabel = "균형 추천",
                routeColorArgb = 0xFF356FB3,
                coordinates = balancedRouteCoordinates
            ),
            RouteCandidate(
                id = "fast",
                title = "빠른 경로",
                distanceMeters = 1400,
                durationMinutes = 18,
                airScore = 62,
                exposureSummary = "가장 빠르지만 차량 통행량이 많은 구간이 포함될 수 있습니다.",
                highlightLabel = "시간 우선",
                routeColorArgb = 0xFF8A6A2A,
                coordinates = fastRouteCoordinates
            )
        )

        return routeScorer.rankRoutes(
            routes = sampleRoutes,
            stations = SampleGyeonggiAirQualityData.stations,
            readings = SampleGyeonggiAirQualityData.latestReadings
        )
    }

    private val cleanRouteCoordinates = listOf(
        GeoPoint(37.2636, 127.0286),
        GeoPoint(37.2665, 127.0301),
        GeoPoint(37.2704, 127.0332),
        GeoPoint(37.2752, 127.0398),
        GeoPoint(37.2794, 127.0471)
    )

    private val balancedRouteCoordinates = listOf(
        GeoPoint(37.2636, 127.0286),
        GeoPoint(37.2660, 127.0317),
        GeoPoint(37.2698, 127.0362),
        GeoPoint(37.2743, 127.0424),
        GeoPoint(37.2794, 127.0471)
    )

    private val fastRouteCoordinates = listOf(
        GeoPoint(37.2636, 127.0286),
        GeoPoint(37.2678, 127.0341),
        GeoPoint(37.2738, 127.0418),
        GeoPoint(37.2794, 127.0471)
    )

    private val hanamPreviewCoordinates = listOf(
        GeoPoint(37.56309735, 127.19289048),
        GeoPoint(37.5603, 127.1931),
        GeoPoint(37.5578, 127.1935),
        GeoPoint(37.55570933, 127.19391838)
    )
}
