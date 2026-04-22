package com.example.myweather.ui.citylist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myweather.R
import com.example.myweather.data.City
import com.example.myweather.data.CityDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder {
    NAME_ASC, NAME_DESC, TEMP_ASC, TEMP_DESC
}

class CityViewModel(
    private val app: Application,
    private val dao: CityDao
) : AndroidViewModel(app) {

    val searchQuery = MutableStateFlow("")
    val sortOrder = MutableStateFlow(SortOrder.NAME_ASC)
    val weatherFilter = MutableStateFlow<String?>(null)

    private val allCities = dao.getAllCities()

    val cities: StateFlow<List<City>> = combine(
        allCities,
        searchQuery,
        sortOrder,
        weatherFilter
    ) { cities, query, sort, filter ->
        var filteredList = if (query.isBlank()) {
            cities
        } else {
            cities.filter { fuzzyMatchWithLocalization(it, query) }
        }

        if (filter != null) {
            filteredList = filteredList.filter { it.weatherState.equals(filter, ignoreCase = true) }
        }

        when (sort) {
            SortOrder.NAME_ASC -> filteredList.sortedBy { it.name }
            SortOrder.NAME_DESC -> filteredList.sortedByDescending { it.name }
            SortOrder.TEMP_ASC -> filteredList.sortedBy { it.temperature }
            SortOrder.TEMP_DESC -> filteredList.sortedByDescending { it.temperature }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mainCity: StateFlow<City?> = dao.getMainCity()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private fun fuzzyMatchWithLocalization(city: City, query: String): Boolean {
        val q = query.lowercase()
        
        // 1. Check canonical English name
        if (fuzzyMatch(city.name, q)) return true
        
        // 2. Check localized name (Cyrillic etc.)
        val resId = app.resources.getIdentifier(
            "city_${city.name.lowercase()}",
            "string",
            app.packageName
        )
        if (resId != 0) {
            val localizedName = app.getString(resId)
            if (fuzzyMatch(localizedName, q)) return true
        }
        
        return false
    }

    private fun fuzzyMatch(text: String, query: String): Boolean {
        val t = text.lowercase()
        val q = query.lowercase()
        if (t.contains(q)) return true
        
        var i = 0
        var j = 0
        while (i < t.length && j < q.length) {
            if (t[i] == q[j]) j++
            i++
        }
        return j == q.length
    }

    fun setMainCity(city: City) = viewModelScope.launch { dao.setMainCity(city.id) }
    fun insertCity(city: City) = viewModelScope.launch { dao.insertCity(city) }
    fun updateCity(city: City) = viewModelScope.launch { dao.updateCity(city) }
    fun deleteCity(city: City) = viewModelScope.launch { dao.deleteCity(city) }
}

class CityViewModelFactory(
    private val application: Application,
    private val dao: CityDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CityViewModel(application, dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}