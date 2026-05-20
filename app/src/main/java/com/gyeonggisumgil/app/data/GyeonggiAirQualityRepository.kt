package com.gyeonggisumgil.app.data

import com.gyeonggisumgil.app.domain.model.AirQualityReading
import com.gyeonggisumgil.app.domain.model.AirQualityStation

interface GyeonggiAirQualityRepository {
    suspend fun getStations(): List<AirQualityStation>
    suspend fun getLatestReadings(): List<AirQualityReading>
}
