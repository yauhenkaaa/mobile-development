package com.example.myweather.network

import com.google.gson.annotations.SerializedName

data class OpenWeatherResponse(
    @SerializedName("name") val name: String,
    @SerializedName("sys") val sys: SysDto,
    @SerializedName("main") val main: MainDto,
    @SerializedName("weather") val weather: List<WeatherDto>
)

data class SysDto(
    @SerializedName("country") val country: String
)

data class MainDto(
    @SerializedName("temp") val temp: Double
)

data class WeatherDto(
    @SerializedName("main") val main: String,
    @SerializedName("description") val description: String
)
