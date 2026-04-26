package com.example.myweather.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_records")
data class WeatherRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firestoreId: String? = null,
    val userId: String = "",
    val cityName: String = "",
    val country: String = "",
    val temperature: Double = 0.0,
    val weatherState: String = "",
    val recordedAt: Long = 0L
)
