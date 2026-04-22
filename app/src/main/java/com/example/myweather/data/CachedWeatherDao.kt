package com.example.myweather.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedWeatherDao {
    @Query("SELECT * FROM cached_weather LIMIT 1")
    fun observeCachedWeather(): Flow<CachedWeather?>

    @Query("SELECT * FROM cached_weather LIMIT 1")
    suspend fun getCachedWeather(): CachedWeather?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedWeather(cachedWeather: CachedWeather)

    @Query("DELETE FROM cached_weather")
    suspend fun clearCache()
}
