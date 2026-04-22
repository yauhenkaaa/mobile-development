package com.example.myweather.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherRecordDao {
    @Query("SELECT * FROM weather_records ORDER BY recordedAt DESC")
    fun observeRecords(): Flow<List<WeatherRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: WeatherRecord): Long

    @Update
    suspend fun updateRecord(record: WeatherRecord)
}
