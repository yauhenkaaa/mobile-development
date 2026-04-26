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

    @Query("SELECT * FROM weather_records WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getRecordByFirestoreId(firestoreId: String): WeatherRecord?

    @Query("SELECT * FROM weather_records WHERE cityName = :cityName AND recordedAt = :recordedAt LIMIT 1")
    suspend fun getRecordByData(cityName: String, recordedAt: Long): WeatherRecord?

    @Query("UPDATE weather_records SET userId = :userId WHERE userId = '' OR userId IS NULL")
    suspend fun updateEmptyUserIds(userId: String)
}
