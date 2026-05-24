package com.gyeonggisumgil.app.data.airkorea

import android.util.Log
import com.gyeonggisumgil.app.data.tmap.TmapPlaceResolver
import com.gyeonggisumgil.app.domain.model.AirQualityGrade
import com.gyeonggisumgil.app.domain.model.AirQualityReading
import com.gyeonggisumgil.app.domain.model.AirQualityStation
import com.gyeonggisumgil.app.domain.model.GeoPoint

class AirKoreaCurrentAirQualityRepository(
    private val api: AirKoreaApi,
    private val placeResolver: TmapPlaceResolver?
) {
    fun getCurrentAirQuality(point: GeoPoint): CurrentAirQualityResult? {
        val address = runCatching {
            placeResolver?.reverseGeocode(point)
        }.getOrNull().orEmpty()
        Log.d(APP_LOG_TAG, "AirKorea reverse address=$address")
        val supportedSido = address.supportedAirKoreaSidoName()
        if (supportedSido == null) {
            Log.w(APP_LOG_TAG, "AirKorea unsupported address=$address")
            return null
        }

        val measurements = api.getMeasurements(supportedSido)
        if (measurements.isEmpty()) return null

        val directMatch = measurements.bestMatchFor(address)
        val cityMatch = if (directMatch == null && supportedSido == "경기") {
            measurements.bestCityFallbackFor(address)
        } else {
            null
        }
        val selected = directMatch ?: cityMatch
        if (selected == null) {
            Log.w(
                APP_LOG_TAG,
                "AirKorea station match failed. sido=$supportedSido, address=$address, stations=${measurements.take(30).joinToString { it.stationName }}"
            )
            return null
        }
        if (cityMatch != null) {
            Log.d(APP_LOG_TAG, "AirKorea city fallback station=${cityMatch.stationName}, address=$address")
        }
        val displayCity = address.airKoreaDisplayRegionName()

        return CurrentAirQualityResult(
            station = AirQualityStation(
                id = "airkorea-${selected.stationName}",
                name = selected.stationName,
                city = displayCity,
                address = address.ifBlank { "$displayCity ${selected.stationName}" },
                latitude = point.latitude,
                longitude = point.longitude
            ),
            reading = AirQualityReading(
                stationId = "airkorea-${selected.stationName}",
                measuredAt = selected.dataTime,
                pm10 = selected.pm10Value,
                pm25 = selected.pm25Value,
                grade = selected.grade()
            ),
            address = address,
            measurementCount = measurements.size
        )
    }

    private fun List<AirKoreaMeasurement>.bestMatchFor(address: String): AirKoreaMeasurement? {
        val normalizedAddress = address.normalizedKoreanLocation()
        if (normalizedAddress.isBlank()) return null

        return maxByOrNull { measurement ->
            measurement.stationName.matchScore(normalizedAddress)
        }?.takeIf { measurement ->
            measurement.stationName.matchScore(normalizedAddress) > 0
        }
    }

    private fun List<AirKoreaMeasurement>.bestCityFallbackFor(address: String): AirKoreaMeasurement? {
        val normalizedAddress = address.normalizedKoreanLocation()
        if (normalizedAddress.isBlank()) return null

        val stationNames = CITY_STATION_FALLBACKS
            .firstNotNullOfOrNull { (cityName, stationNames) ->
                stationNames.takeIf { normalizedAddress.contains(cityName.normalizedKoreanLocation()) }
            }
            ?: return null

        return stationNames.firstNotNullOfOrNull { stationName ->
            firstOrNull { measurement -> measurement.stationName == stationName }
        }
    }

    private fun String.matchScore(address: String): Int {
        val normalizedStation = normalizedKoreanLocation()
        if (normalizedStation.length < 2) return 0

        var score = 0
        if (address.contains(normalizedStation)) score += 100
        normalizedStation.locationTokens().forEach { token ->
            if (address.contains(token)) score += token.length
        }
        return score
    }

    private fun String.normalizedKoreanLocation(): String {
        return replace("경기도", "")
            .replace("경기", "")
            .replace("특별시", "")
            .replace("광역시", "")
            .replace("시", "")
            .replace("군", "")
            .replace("구", "")
            .replace("동", "")
            .replace("읍", "")
            .replace("면", "")
            .replace(" ", "")
            .trim()
    }

    private fun String.locationTokens(): List<String> {
        return split(Regex("[\\s·,()\\-]+"))
            .map { it.normalizedKoreanLocation() }
            .filter { it.length >= 2 }
    }

    private fun String.supportedAirKoreaSidoName(): String? {
        return when {
            contains("경기도") || contains("경기 ") -> "경기"
            contains("서울특별시") || contains("서울 ") -> "서울"
            contains("인천광역시") || contains("인천 ") -> "인천"
            else -> null
        }
    }

    private fun String.airKoreaDisplayRegionName(): String {
        return Regex("""(경기도|서울특별시|인천광역시)\s+([^\s]+?(?:시|군|구))""")
            .find(this)
            ?.groupValues
            ?.takeIf { it.size >= 3 }
            ?.let { "${it[1]} ${it[2]}" }
            ?: when (supportedAirKoreaSidoName()) {
                "경기" -> "경기도"
                "서울" -> "서울특별시"
                "인천" -> "인천광역시"
                else -> "경기 인근"
            }
    }

    private fun AirKoreaMeasurement.grade(): AirQualityGrade {
        val worstGrade = listOfNotNull(khaiGrade, pm10Grade, pm25Grade).maxOrNull()
        return when (worstGrade) {
            1 -> AirQualityGrade.GOOD
            2 -> AirQualityGrade.NORMAL
            3 -> AirQualityGrade.BAD
            4 -> AirQualityGrade.VERY_BAD
            else -> gradeFromValues(pm10Value, pm25Value)
        }
    }

    private fun gradeFromValues(pm10: Int?, pm25: Int?): AirQualityGrade {
        val pm10Grade = when {
            pm10 == null -> AirQualityGrade.UNKNOWN
            pm10 <= 30 -> AirQualityGrade.GOOD
            pm10 <= 80 -> AirQualityGrade.NORMAL
            pm10 <= 150 -> AirQualityGrade.BAD
            else -> AirQualityGrade.VERY_BAD
        }
        val pm25Grade = when {
            pm25 == null -> AirQualityGrade.UNKNOWN
            pm25 <= 15 -> AirQualityGrade.GOOD
            pm25 <= 35 -> AirQualityGrade.NORMAL
            pm25 <= 75 -> AirQualityGrade.BAD
            else -> AirQualityGrade.VERY_BAD
        }
        return listOf(pm10Grade, pm25Grade).maxByOrNull { it.priority } ?: AirQualityGrade.UNKNOWN
    }

    private val AirQualityGrade.priority: Int
        get() = when (this) {
            AirQualityGrade.UNKNOWN -> 0
            AirQualityGrade.GOOD -> 1
            AirQualityGrade.NORMAL -> 2
            AirQualityGrade.BAD -> 3
            AirQualityGrade.VERY_BAD -> 4
        }

    private companion object {
        val CITY_STATION_FALLBACKS = linkedMapOf(
            "부천시" to listOf("소사본동", "내동", "중2동", "오정동", "송내대로(중동)"),
            "안산시" to listOf("고잔동", "중앙대로(고잔동)", "초지동", "본오동", "원곡동", "부곡동1", "대부동", "호수동"),
            "과천시" to listOf("별양동", "과천동"),
            "구리시" to listOf("교문동", "동구동"),
            "의왕시" to listOf("고천동", "부곡3동"),
            "시흥시" to listOf("정왕동", "시화산단", "대야동", "목감동", "장현동", "서해안로", "배곧동"),
            "남양주시" to listOf("금곡동", "오남읍", "별내동", "화도읍", "경춘로", "와부읍", "진접읍"),
            "평택시" to listOf("비전동", "안중", "평택항", "송북동", "청북읍", "고덕동"),
            "파주시" to listOf("금촌동", "운정", "파주", "파주읍"),
            "고양시" to listOf("행신동", "식사동", "백마로(마두역)", "신원동", "주엽동"),
            "광주시" to listOf("경안동", "오포1동", "곤지암"),
            "용인시" to listOf("김량장동", "수지", "기흥", "중부대로(구갈동)", "모현읍", "이동읍", "백암면"),
            "이천시" to listOf("설성면", "창전동", "장호원읍", "부발읍"),
            "포천시" to listOf("관인면", "선단동", "일동면"),
            "김포시" to listOf("사우동", "고촌읍", "월곶면", "한강신도시", "한강로"),
            "군포시" to listOf("당동", "산본동"),
            "오산시" to listOf("오산동", "금암로(신장동)"),
            "하남시" to listOf("미사", "신장동", "감일"),
            "화성시" to listOf("남양읍", "향남읍", "동탄", "우정읍", "청계동", "새솔동", "봉담읍", "서신면"),
            "양주시" to listOf("백석읍", "고읍"),
            "동두천시" to listOf("보산동"),
            "안성시" to listOf("봉산동", "공도읍", "죽산면"),
            "여주시" to listOf("중앙동(경기)", "대신면", "가남읍"),
            "연천군" to listOf("연천", "전곡", "연천(DMZ)"),
            "가평군" to listOf("가평"),
            "양평군" to listOf("용문면", "양평읍"),
            "수원시" to listOf("인계동", "광교동", "영통동", "천천동", "경수대로(동수원)", "고색동", "호매실", "신풍동"),
            "성남시" to listOf("대왕판교로(백현동)", "단대동", "정자동", "수내동", "성남대로(모란역)", "복정동", "운중동", "상대원동"),
            "의정부시" to listOf("의정부동", "의정부1동", "송산3동"),
            "안양시" to listOf("안양8동", "부림동", "호계3동", "안양2동"),
            "광명시" to listOf("철산동", "소하동")
        )
    }
}

data class CurrentAirQualityResult(
    val station: AirQualityStation,
    val reading: AirQualityReading,
    val address: String,
    val measurementCount: Int
)

private const val APP_LOG_TAG = "GyeonggiSumgil"
