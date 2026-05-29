package com.gyeonggisumgil.app.data.ai

import com.gyeonggisumgil.app.data.walking.GyeonggiWalkingTrail
import com.gyeonggisumgil.app.data.walking.WalkingTrailRecommendation
import com.gyeonggisumgil.app.domain.model.GeoPoint
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

enum class AiCourseShape {
    LakeLoop,
    RiverOutAndBack,
    ParkLoop
}

data class AiWalkingCourse(
    val id: String,
    val name: String,
    val aliases: List<String>,
    val shape: AiCourseShape,
    val center: GeoPoint,
    val entryPoint: GeoPoint,
    val waypoints: List<GeoPoint>,
    val sourceTrailId: String? = null
)

data class AiWalkingCourseSelection(
    val course: AiWalkingCourse,
    val explicitPlaceRequested: Boolean
)

class AiWalkingCourseCatalog {
    fun selectCourse(
        query: String,
        currentLocation: GeoPoint?,
        nearbyRecommendations: List<WalkingTrailRecommendation>
    ): AiWalkingCourseSelection? {
        val normalizedQuery = query.normalizedKeyword()
        courses.firstOrNull { course ->
            course.aliases.any { alias -> normalizedQuery.contains(alias.normalizedKeyword()) }
        }?.let { course ->
            return AiWalkingCourseSelection(course = course, explicitPlaceRequested = true)
        }

        val nearbyTrailIds = nearbyRecommendations.map { it.trail.id }
        nearbyTrailIds.firstNotNullOfOrNull { trailId ->
            courses.firstOrNull { course -> course.sourceTrailId == trailId }
        }?.let { course ->
            return AiWalkingCourseSelection(course = course, explicitPlaceRequested = false)
        }

        return currentLocation?.let { location ->
            courses.minByOrNull { course -> location.approximateDistanceTo(course.center) }
                ?.let { course -> AiWalkingCourseSelection(course = course, explicitPlaceRequested = false) }
        }
    }

    fun matchingTrail(course: AiWalkingCourse): GyeonggiWalkingTrail? {
        val trailId = course.sourceTrailId ?: return null
        return nearbyTrailById[trailId]
    }

    companion object {
        private val nearbyTrailById = GyeonggiWalkingTrailRepositoryMirror.trails.associateBy { it.id }

        val courses = listOf(
            AiWalkingCourse(
                id = "gwanggyo-lake-loop",
                name = "광교호수공원 원천호수 한바퀴",
                aliases = listOf("광교호수", "광교 호수", "광교호수공원", "원천호수", "신대호수"),
                shape = AiCourseShape.LakeLoop,
                center = GeoPoint(37.2831, 127.0605),
                entryPoint = GeoPoint(37.2798, 127.0577),
                waypoints = listOf(
                    GeoPoint(37.2813, 127.0564),
                    GeoPoint(37.2836, 127.0562),
                    GeoPoint(37.2856, 127.0571),
                    GeoPoint(37.2871, 127.0592),
                    GeoPoint(37.2873, 127.0618),
                    GeoPoint(37.2862, 127.0638),
                    GeoPoint(37.2841, 127.0645),
                    GeoPoint(37.2819, 127.0643),
                    GeoPoint(37.2801, 127.0630),
                    GeoPoint(37.2790, 127.0610),
                    GeoPoint(37.2788, 127.0593)
                ),
                sourceTrailId = "suwon-gwanggyo-lake"
            ),
            AiWalkingCourse(
                id = "misa-lake-loop",
                name = "미사호수공원 한바퀴",
                aliases = listOf("미사호수", "미사 호수", "미사호수공원", "망월천"),
                shape = AiCourseShape.LakeLoop,
                center = GeoPoint(37.5639, 127.1907),
                entryPoint = GeoPoint(37.5630, 127.1929),
                waypoints = listOf(
                    GeoPoint(37.5660, 127.1908),
                    GeoPoint(37.5647, 127.1872),
                    GeoPoint(37.5609, 127.1876),
                    GeoPoint(37.5587, 127.1917),
                    GeoPoint(37.5608, 127.1944)
                ),
                sourceTrailId = "hanam-misa-lake"
            ),
            AiWalkingCourse(
                id = "misa-han-river-outback",
                name = "미사 한강변 왕복 산책",
                aliases = listOf("미사강변대로", "미사IC", "미사 한강", "미사 한강변", "미사한강공원", "한강공원 미사", "미사강변"),
                shape = AiCourseShape.RiverOutAndBack,
                center = GeoPoint(37.5585, 127.2060),
                entryPoint = GeoPoint(37.5488, 127.2135),
                waypoints = listOf(
                    GeoPoint(37.5527, 127.2110),
                    GeoPoint(37.5572, 127.2070),
                    GeoPoint(37.5618, 127.2028),
                    GeoPoint(37.5664, 127.1987)
                )
            ),
            AiWalkingCourse(
                id = "jungnangcheon-river",
                name = "중랑천 왕복 산책",
                aliases = listOf("중랑천", "중랑천길", "월계", "하계천"),
                shape = AiCourseShape.RiverOutAndBack,
                center = GeoPoint(37.6240, 127.0670),
                entryPoint = GeoPoint(37.6155, 127.0738),
                waypoints = listOf(
                    GeoPoint(37.6251, 127.0687),
                    GeoPoint(37.6336, 127.0647),
                    GeoPoint(37.6251, 127.0687)
                )
            ),
            AiWalkingCourse(
                id = "byeollae-wangsukcheon-river",
                name = "별내 왕숙천 왕복 산책",
                aliases = listOf("별내 왕숙천", "별내역", "별내별가람", "별내 별가람", "남양주 왕숙천"),
                shape = AiCourseShape.RiverOutAndBack,
                center = GeoPoint(37.6552, 127.1211),
                entryPoint = GeoPoint(37.6427, 127.1269),
                waypoints = listOf(
                    GeoPoint(37.6468, 127.1248),
                    GeoPoint(37.6512, 127.1222),
                    GeoPoint(37.6565, 127.1198),
                    GeoPoint(37.6617, 127.1172),
                    GeoPoint(37.6668, 127.1149)
                )
            ),
            AiWalkingCourse(
                id = "wangsukcheon-river",
                name = "왕숙천 왕복 산책",
                aliases = listOf("왕숙천", "구리 왕숙천", "토평", "인창천"),
                shape = AiCourseShape.RiverOutAndBack,
                center = GeoPoint(37.6017, 127.1438),
                entryPoint = GeoPoint(37.5968, 127.1435),
                waypoints = listOf(
                    GeoPoint(37.6042, 127.1450),
                    GeoPoint(37.6110, 127.1461),
                    GeoPoint(37.6042, 127.1450)
                ),
                sourceTrailId = "guri-wangsukcheon"
            )
        )
    }
}

private fun String.normalizedKeyword(): String {
    return lowercase()
        .replace(" ", "")
        .replace("-", "")
        .replace("_", "")
}

private fun GeoPoint.approximateDistanceTo(other: GeoPoint): Double {
    val latitudeScale = 111_000.0
    val longitudeScale = latitudeScale * cos(Math.toRadians((latitude + other.latitude) / 2.0))
    val latitudeDistance = (latitude - other.latitude) * latitudeScale
    val longitudeDistance = (longitude - other.longitude) * longitudeScale

    return sqrt(latitudeDistance.pow(2) + longitudeDistance.pow(2))
}

private object GyeonggiWalkingTrailRepositoryMirror {
    val trails = com.gyeonggisumgil.app.data.walking.GyeonggiWalkingTrailRepository.trails
}
