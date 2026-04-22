package com.example.myweather.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_weather")
data class CachedWeather(
    @PrimaryKey val cityId: Int,
    val cityName: String,
    val country: String,
    val temperature: Double,
    val weatherState: String,
    val updatedAt: Long
)
