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
        return getRecommendedRoutes(startPlace, emptyList(), destinationPlace)
    }

    fun getRecommendedRoutes(
        startPlace: TmapPlace,
        waypointPlace: TmapPlace?,
        destinationPlace: TmapPlace
    ): List<RouteCandidate> {
        return getRecommendedRoutes(startPlace, listOfNotNull(waypointPlace), destinationPlace)
    }

    fun getRecommendedRoutes(
        startPlace: TmapPlace,
        waypointPlaces: List<TmapPlace>,
        destinationPlace: TmapPlace
    ): List<RouteCandidate> {
        val routes = listOf(
            getRoute(
                startPlace = startPlace,
                waypointPlaces = waypointPlaces,
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
                waypointPlaces = waypointPlaces,
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
        waypointPlaces: List<TmapPlace>,
        destinationPlace: TmapPlace,
        searchOption: SearchOption,
        id: String,
        title: String,
        highlightLabel: String,
        routeColorArgb: Long,
        baseAirScore: Int
    ): RouteCandidate {
        if (waypointPlaces.isEmpty()) {
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

        val stops = listOf(startPlace) + waypointPlaces + destinationPlace
        val legs = stops.zipWithNext().mapIndexed { index, (legStart, legDestination) ->
            parseRoute(
                request = TmapPedestrianRouteRequest(
                    start = legStart,
                    destination = legDestination,
                    searchOption = searchOption
                ),
                id = "$id-leg-$index",
                title = title,
                highlightLabel = highlightLabel,
                routeColorArgb = routeColorArgb,
                baseAirScore = baseAirScore
            )
        }
        val firstLeg = legs.first()
        val combinedCoordinates = legs.flatMapIndexed { index, leg ->
            if (index == 0) leg.coordinates else leg.coordinates.drop(1)
        }

        return firstLeg.copy(
            id = id,
            distanceMeters = legs.sumOf { it.distanceMeters },
            durationMinutes = legs.sumOf { it.durationMinutes },
            exposureSummary = waypointPlaces.toWaypointSummary(),
            coordinates = combinedCoordinates
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

private fun List<TmapPlace>.toWaypointSummary(): String {
    return when (size) {
        0 -> "Tmap 보행자 경로안내 API로 계산한 도보 경로입니다."
        1 -> "${first().name}을 지나 돌아오는 산책 경로입니다."
        else -> "산책 지점을 따라 돌아오는 순환형 경로입니다."
    }
}
