package com.gyeonggisumgil.app.data

import com.gyeonggisumgil.app.domain.model.AirQualityGrade
import com.gyeonggisumgil.app.domain.model.AirQualityReading
import com.gyeonggisumgil.app.domain.model.AirQualityStation

object SampleGyeonggiAirQualityData {
    val stations = listOf(
        AirQualityStation(
            id = "suwon-paldal",
            name = "팔달구",
            city = "수원시",
            address = "경기도 수원시 팔달구",
            latitude = 37.2636,
            longitude = 127.0286
        ),
        AirQualityStation(
            id = "suwon-maetan",
            name = "매탄권선",
            city = "수원시",
            address = "경기도 수원시 영통구 매탄동",
            latitude = 37.2719,
            longitude = 127.0397
        ),
        AirQualityStation(
            id = "suwon-gwanggyo",
            name = "광교",
            city = "수원시",
            address = "경기도 수원시 영통구 광교",
            latitude = 37.2794,
            longitude = 127.0471
        ),
        AirQualityStation(
            id = "hanam-misa",
            name = "미사",
            city = "하남시",
            address = "경기도 하남시 미사",
            latitude = 37.5631,
            longitude = 127.1929
        )
    )

    val latestReadings = listOf(
        AirQualityReading(
            stationId = "suwon-paldal",
            measuredAt = "2026-05-21 15:00",
            pm10 = 42,
            pm25 = 21,
            grade = AirQualityGrade.NORMAL
        ),
        AirQualityReading(
            stationId = "suwon-maetan",
            measuredAt = "2026-05-21 15:00",
            pm10 = 58,
            pm25 = 31,
            grade = AirQualityGrade.BAD
        ),
        AirQualityReading(
            stationId = "suwon-gwanggyo",
            measuredAt = "2026-05-21 15:00",
            pm10 = 34,
            pm25 = 16,
            grade = AirQualityGrade.NORMAL
        ),
        AirQualityReading(
            stationId = "hanam-misa",
            measuredAt = "2026-05-21 15:00",
            pm10 = 39,
            pm25 = 19,
            grade = AirQualityGrade.NORMAL
        )
    )

    val dashboardReading = latestReadings.first()
}
