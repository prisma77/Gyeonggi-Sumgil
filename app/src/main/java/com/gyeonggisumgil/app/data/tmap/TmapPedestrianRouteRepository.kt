package com.gyeonggisumgil.app.data.tmap

import com.gyeonggisumgil.app.data.RouteRepository
import com.gyeonggisumgil.app.data.SampleGyeonggiAirQualityData
import com.gyeonggisumgil.app.domain.model.RouteCandidate
import com.gyeonggisumgil.app.domain.route.AirAwareRouteScorer

class TmapPedestrianRouteRepository(
    private val api: TmapPedestrianRouteApi,
    private val parser: TmapPedestrianRouteParser = TmapPedestrianRouteParser(),
    private val placeResolver: TmapPlaceResolver = TmapPlaceResolver(api.appKey),
    private val routeScorer: AirAwareRouteScorer = AirAwareRouteScorer()
) : RouteRepository {
    override fun getRecommendedRoutes(
        start: String,
        destination: String
    ): List<RouteCandidate> {
        val startPlace = placeResolver.resolve(start) ?: return emptyList()
        val destinationPlace = placeResolver.resolve(destination) ?: return emptyList()

        return getRecommendedRoutes(startPlace, destinationPlace)
    }

    fun getRecommendedRoutes(
        start: String,
        waypoint: String?,
        destination: String
    ): List<RouteCandidate> {
        val startPlace = placeResolver.resolve(start) ?: return emptyList()
        val destinationPlace = placeResolver.resolve(destination) ?: return emptyList()
        val waypointPlace = waypoint
            ?.takeIf { it.isNotBlank() }
            ?.let { placeResolver.resolve(it) ?: return emptyList() }

        return getRecommendedRoutes(startPlace, waypointPlace, destinationPlace)
    }

    fun getRecommendedRoutes(
        startPlace: TmapPlace,
        destinationPlace: TmapPlace
    ): List<RouteCandidate> {
        return getRecommendedRoutes(startPlace, null, destinationPlace)
    }

    fun getRecommendedRoutes(
        startPlace: TmapPlace,
        waypointPlace: TmapPlace?,
        destinationPlace: TmapPlace
    ): List<RouteCandidate> {
        val routes = listOf(
            getRoute(
                startPlace = startPlace,
                waypointPlace = waypointPlace,
                destinationPlace = destinationPlace,
                searchOption = SearchOption.AvoidStairs,
                id = "tmap-avoid-stairs",
                title = "산책 경로",
                highlightLabel = "산책 우선",
                routeColorArgb = 0xFF2E7D5B,
                baseAirScore = 86
            ),
            getRoute(
                startPlace = startPlace,
                waypointPlace = waypointPlace,
                destinationPlace = destinationPlace,
                searchOption = SearchOption.Recommended,
                id = "tmap-recommended",
                title = "큰 길 위주 경로",
                highlightLabel = "큰 길 위주",
                routeColorArgb = 0xFF356FB3,
                baseAirScore = 80
            )
        )

        return routeScorer.rankRoutes(
            routes = routes,
            stations = SampleGyeonggiAirQualityData.stations,
            readings = SampleGyeonggiAirQualityData.latestReadings
        )
    }

    private fun getRoute(
        startPlace: TmapPlace,
        waypointPlace: TmapPlace?,
        destinationPlace: TmapPlace,
        searchOption: SearchOption,
        id: String,
        title: String,
        highlightLabel: String,
        routeColorArgb: Long,
        baseAirScore: Int
    ): RouteCandidate {
        if (waypointPlace == null) {
            return parseRoute(
                request = TmapPedestrianRouteRequest(
                    start = startPlace,
                    destination = destinationPlace,
                    searchOption = searchOption
                ),
                id = id,
                title = title,
                highlightLabel = highlightLabel,
                routeColorArgb = routeColorArgb,
                baseAirScore = baseAirScore
            )
        }

        val firstLeg = parseRoute(
            request = TmapPedestrianRouteRequest(
                start = startPlace,
                destination = waypointPlace,
                searchOption = searchOption
            ),
            id = "$id-first-leg",
            title = title,
            highlightLabel = highlightLabel,
            routeColorArgb = routeColorArgb,
            baseAirScore = baseAirScore
        )
        val secondLeg = parseRoute(
            request = TmapPedestrianRouteRequest(
                start = waypointPlace,
                destination = destinationPlace,
                searchOption = searchOption
            ),
            id = "$id-second-leg",
            title = title,
            highlightLabel = highlightLabel,
            routeColorArgb = routeColorArgb,
            baseAirScore = baseAirScore
        )

        return firstLeg.copy(
            id = id,
            distanceMeters = firstLeg.distanceMeters + secondLeg.distanceMeters,
            durationMinutes = firstLeg.durationMinutes + secondLeg.durationMinutes,
            exposureSummary = "경유지 ${waypointPlace.name}을 거치는 산책 경로입니다.",
            coordinates = firstLeg.coordinates + secondLeg.coordinates.drop(1)
        )
    }

    private fun parseRoute(
        request: TmapPedestrianRouteRequest,
        id: String,
        title: String,
        highlightLabel: String,
        routeColorArgb: Long,
        baseAirScore: Int
    ): RouteCandidate {
        return parser.parse(
            responseJson = api.getPedestrianRoute(request),
            routeId = id,
            title = title,
            highlightLabel = highlightLabel,
            routeColorArgb = routeColorArgb,
            fallbackSummary = "Tmap 보행자 경로안내 API로 계산한 도보 경로입니다.",
            baseAirScore = baseAirScore
        )
    }

}
