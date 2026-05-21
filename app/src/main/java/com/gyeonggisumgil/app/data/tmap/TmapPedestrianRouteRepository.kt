package com.gyeonggisumgil.app.data.tmap

import com.gyeonggisumgil.app.data.RouteRepository
import com.gyeonggisumgil.app.domain.model.RouteCandidate

class TmapPedestrianRouteRepository(
    private val api: TmapPedestrianRouteApi,
    private val parser: TmapPedestrianRouteParser = TmapPedestrianRouteParser()
) : RouteRepository {
    override fun getRecommendedRoutes(
        start: String,
        destination: String
    ): List<RouteCandidate> {
        val startPlace = knownPlaces[start] ?: return emptyList()
        val destinationPlace = knownPlaces[destination] ?: return emptyList()

        return listOf(
            getRoute(
                request = TmapPedestrianRouteRequest(
                    start = startPlace,
                    destination = destinationPlace,
                    searchOption = SearchOption.Recommended
                ),
                id = "tmap-recommended",
                title = "Tmap 추천 도보 경로",
                highlightLabel = "도보 추천",
                routeColorArgb = 0xFF2E7D5B
            ),
            getRoute(
                request = TmapPedestrianRouteRequest(
                    start = startPlace,
                    destination = destinationPlace,
                    searchOption = SearchOption.AvoidStairs
                ),
                id = "tmap-avoid-stairs",
                title = "계단 회피 경로",
                highlightLabel = "계단 회피",
                routeColorArgb = 0xFF356FB3
            )
        )
    }

    private fun getRoute(
        request: TmapPedestrianRouteRequest,
        id: String,
        title: String,
        highlightLabel: String,
        routeColorArgb: Long
    ): RouteCandidate {
        return parser.parse(
            responseJson = api.getPedestrianRoute(request),
            routeId = id,
            title = title,
            highlightLabel = highlightLabel,
            routeColorArgb = routeColorArgb,
            fallbackSummary = "Tmap 보행자 경로안내 API로 계산한 도보 경로입니다."
        )
    }

    companion object {
        private val knownPlaces = mapOf(
            "수원시청" to TmapPlace(
                name = "수원시청",
                longitude = 127.0286,
                latitude = 37.2636
            ),
            "광교호수공원" to TmapPlace(
                name = "광교호수공원",
                longitude = 127.0471,
                latitude = 37.2794
            )
        )
    }
}
