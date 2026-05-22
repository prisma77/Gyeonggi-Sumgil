package com.gyeonggisumgil.app.data.airkorea

data class AirKoreaMeasurement(
    val stationName: String,
    val dataTime: String,
    val pm10Value: Int?,
    val pm25Value: Int?,
    val khaiGrade: Int?,
    val pm10Grade: Int?,
    val pm25Grade: Int?
)
