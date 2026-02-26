package com.example.myweather.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cities")
data class City(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val country: String,
    val temperature: Double,
    val weatherState: String,
    @ColumnInfo(defaultValue = "0")
    val isMain: Boolean = false
)