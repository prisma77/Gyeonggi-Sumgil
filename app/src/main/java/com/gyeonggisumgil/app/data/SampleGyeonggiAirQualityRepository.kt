package com.gyeonggisumgil.app.data

import com.gyeonggisumgil.app.domain.model.AirQualityReading
import com.gyeonggisumgil.app.domain.model.AirQualityStation

class SampleGyeonggiAirQualityRepository : GyeonggiAirQualityRepository {
    override suspend fun getStations(): List<AirQualityStation> {
        return SampleGyeonggiAirQualityData.stations
    }

    override suspend fun getLatestReadings(): List<AirQualityReading> {
        return SampleGyeonggiAirQualityData.latestReadings
    }
}
