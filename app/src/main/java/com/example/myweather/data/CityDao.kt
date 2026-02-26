package com.example.myweather.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CityDao {
    @Query("SELECT * FROM cities ORDER BY name ASC")
    fun getAllCities(): Flow<List<City>>

    @Query("SELECT * FROM cities WHERE name LIKE '%' || :searchQuery || '%' ORDER BY name ASC")
    fun searchCities(searchQuery: String): Flow<List<City>>

    @Query("SELECT * FROM cities WHERE id = :id")
    suspend fun getCityById(id: Int): City?

    @Query("SELECT * FROM cities WHERE isMain = 1 LIMIT 1")
    fun getMainCity(): Flow<City?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: City)

    @Update
    suspend fun updateCity(city: City)

    @Delete
    suspend fun deleteCity(city: City)

    @Transaction
    suspend fun setMainCity(cityId: Int) {
        unselectMainCity()
        selectMainCity(cityId)
    }

    @Query("UPDATE cities SET isMain = 0 WHERE isMain = 1")
    suspend fun unselectMainCity()

    @Query("UPDATE cities SET isMain = 1 WHERE id = :cityId")
    suspend fun selectMainCity(cityId: Int)
}