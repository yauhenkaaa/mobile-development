package com.example.myweather

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.example.myweather.data.AppDatabase
import com.example.myweather.data.WeatherRepository
import com.example.myweather.network.OpenWeatherApi
import com.example.myweather.notifications.WeatherNotificationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val weatherApi: OpenWeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenWeatherApi::class.java)
    }

    val weatherRepository: WeatherRepository by lazy {
        WeatherRepository(
            cityDao = database.cityDao(),
            cachedWeatherDao = database.cachedWeatherDao(),
            weatherRecordDao = database.weatherRecordDao(),
            openWeatherApi = weatherApi,
            apiKey = BuildConfig.OPEN_WEATHER_API_KEY
        )
    }

    override fun onCreate() {
        super.onCreate()
        applyTheme()
        WeatherNotificationWorker.scheduleNext(this)
    }

    private fun applyTheme() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val themeValue = sharedPreferences.getString("theme_list", "system")

        val mode = when (themeValue) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}