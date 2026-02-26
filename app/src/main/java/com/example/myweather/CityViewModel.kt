package com.example.myweather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myweather.data.City
import com.example.myweather.data.CityDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CityViewModel(private val dao: CityDao) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val cities: StateFlow<List<City>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                dao.getAllCities()
            } else {
                dao.searchCities(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun insertCity(city: City) = viewModelScope.launch { dao.insertCity(city) }
    fun updateCity(city: City) = viewModelScope.launch { dao.updateCity(city) }
    fun deleteCity(city: City) = viewModelScope.launch { dao.deleteCity(city) }
    suspend fun getCityById(id: Int): City? = dao.getCityById(id)
}

class CityViewModelFactory(private val dao: CityDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CityViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}