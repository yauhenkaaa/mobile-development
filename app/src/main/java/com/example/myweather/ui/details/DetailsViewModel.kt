package com.example.myweather.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myweather.data.City
import com.example.myweather.data.CityDao
import com.example.myweather.data.WeatherRepository
import com.example.myweather.data.WeatherRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class DetailsViewModel(
    private val cityId: Int,
    private val cityDao: CityDao,
    private val repository: WeatherRepository
) : ViewModel() {

    val city: Flow<City> = cityDao.observeCityById(cityId).filterNotNull()

    private val _recordSaved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val recordSaved = _recordSaved

    fun setAsMain() {
        viewModelScope.launch {
            cityDao.setMainCity(cityId)
        }
    }

    fun saveRecord(
        cityName: String,
        country: String,
        temperature: Double,
        weatherState: String,
        recordedAt: Long
    ) {
        viewModelScope.launch {
            repository.addRecord(
                WeatherRecord(
                    cityName = cityName,
                    country = country,
                    temperature = temperature,
                    weatherState = weatherState,
                    recordedAt = recordedAt
                )
            )
            _recordSaved.emit(Unit)
        }
    }
}

class DetailsViewModelFactory(
    private val cityId: Int,
    private val cityDao: CityDao,
    private val repository: WeatherRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DetailsViewModel(cityId, cityDao, repository) as T
    }
}
