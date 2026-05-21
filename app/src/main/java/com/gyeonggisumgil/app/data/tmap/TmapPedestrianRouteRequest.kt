package com.gyeonggisumgil.app.data.tmap

data class TmapPedestrianRouteRequest(
    val start: TmapPlace,
    val destination: TmapPlace,
    val searchOption: SearchOption = SearchOption.Recommended
)

enum class SearchOption(val value: String) {
    Recommended("0"),
    ShortestTime("10"),
    AvoidStairs("30")
}
