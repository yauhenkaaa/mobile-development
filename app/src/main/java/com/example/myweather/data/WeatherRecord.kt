package com.example.myweather.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_records")
data class WeatherRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firestoreId: String? = null,
    val cityName: String,
    val country: String,
    val temperature: Double,
    val weatherState: String,
    val recordedAt: Long
)
