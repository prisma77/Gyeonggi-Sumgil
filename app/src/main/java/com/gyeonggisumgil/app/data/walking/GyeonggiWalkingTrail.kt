package com.gyeonggisumgil.app.data.walking

import com.gyeonggisumgil.app.domain.model.GeoPoint
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

const val GYEONGGI_WALKING_TRAIL_SOURCE_NAME = "경기데이터드림 산책로 현황"

data class GyeonggiWalkingTrail(
    val id: String,
    val sigunName: String,
    val name: String,
    val startAddress: String,
    val endAddress: String,
    val center: GeoPoint,
    val lengthMeters: Int,
    val source: String = GYEONGGI_WALKING_TRAIL_SOURCE_NAME
) {
    val durationMinutes: Int
        get() = (lengthMeters / 75.0).toInt().coerceAtLeast(8)
}

data class WalkingTrailRecommendation(
    val trail: GyeonggiWalkingTrail,
    val typeLabel: String,
    val distanceFromUserMeters: Int?,
    val reason: String
)

class GyeonggiWalkingTrailRepository {
    fun recommendNearbyTrails(
        currentLocation: GeoPoint?,
        airScore: Int?,
        limit: Int = 3
    ): List<WalkingTrailRecommendation> {
        val origin = currentLocation ?: DEFAULT_CENTER
        val score = airScore ?: 75
        val airGuide = when {
            score >= 85 -> "현재 대기질이 좋아 길이별 산책 후보로 적합합니다."
            score >= 70 -> "대기질이 보통이라 짧거나 공원 인접 코스를 우선 추천합니다."
            else -> "대기질이 낮으면 같은 생활권의 다른 산책 후보와 측정소 정보를 함께 확인하세요."
        }

        return trails
            .map { trail -> trail to origin.approximateDistanceTo(trail.center).toInt() }
            .sortedBy { (_, distance) -> distance }
            .take(limit)
            .map { (trail, distance) ->
                WalkingTrailRecommendation(
                    trail = trail,
                    typeLabel = trail.lengthTypeLabel(),
                    distanceFromUserMeters = distance.takeIf { currentLocation != null },
                    reason = "$airGuide ${trail.sigunName} ${trail.name} 위치를 지도에서 확인해 산책 출발점을 잡기 좋습니다."
                )
            }
    }

    private fun GyeonggiWalkingTrail.lengthTypeLabel(): String {
        return when {
            lengthMeters < 1_500 -> "짧은 산책"
            lengthMeters < 3_000 -> "보통 산책"
            else -> "긴 산책"
        }
    }

    private fun GeoPoint.approximateDistanceTo(other: GeoPoint): Double {
        val latitudeScale = 111_000.0
        val longitudeScale = latitudeScale * cos(Math.toRadians((latitude + other.latitude) / 2.0))
        val latitudeDistance = (latitude - other.latitude) * latitudeScale
        val longitudeDistance = (longitude - other.longitude) * longitudeScale

        return sqrt(latitudeDistance.pow(2) + longitudeDistance.pow(2))
    }

    companion object {
        private val DEFAULT_CENTER = GeoPoint(37.5549, 127.1932)

        val trails = listOf(
            GyeonggiWalkingTrail(
                id = "hanam-misa-lake",
                sigunName = "하남시",
                name = "미사호수공원 산책로",
                startAddress = "경기도 하남시 망월동",
                endAddress = "경기도 하남시 미사강변중앙로",
                center = GeoPoint(37.5639, 127.1907),
                lengthMeters = 1_400
            ),
            GyeonggiWalkingTrail(
                id = "guri-wangsukcheon",
                sigunName = "구리시",
                name = "왕숙천 산책로",
                startAddress = "경기도 구리시 인창동",
                endAddress = "경기도 구리시 토평동",
                center = GeoPoint(37.6017, 127.1438),
                lengthMeters = 2_400
            ),
            GyeonggiWalkingTrail(
                id = "suwon-gwanggyo-lake",
                sigunName = "수원시",
                name = "광교호수공원 산책로",
                startAddress = "경기도 수원시 영통구 하동",
                endAddress = "경기도 수원시 영통구 원천동",
                center = GeoPoint(37.2794, 127.0471),
                lengthMeters = 3_200
            ),
            GyeonggiWalkingTrail(
                id = "seongnam-tancheon",
                sigunName = "성남시",
                name = "탄천 산책로",
                startAddress = "경기도 성남시 분당구 정자동",
                endAddress = "경기도 성남시 분당구 수내동",
                center = GeoPoint(37.3698, 127.1144),
                lengthMeters = 2_800
            ),
            GyeonggiWalkingTrail(
                id = "goyang-lake-park",
                sigunName = "고양시",
                name = "호수공원 산책로",
                startAddress = "경기도 고양시 일산동구 장항동",
                endAddress = "경기도 고양시 일산동구 호수로",
                center = GeoPoint(37.6536, 126.7680),
                lengthMeters = 4_000
            ),
            GyeonggiWalkingTrail(
                id = "anyang-suamcheon",
                sigunName = "안양시",
                name = "수암천 산책로",
                startAddress = "경기도 안양시 만안구 안양동",
                endAddress = "경기도 안양시 만안구 병목안로",
                center = GeoPoint(37.3949, 126.9228),
                lengthMeters = 2_100
            )
        )
    }
}
