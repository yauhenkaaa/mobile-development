package com.example.myweather.ui.records

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myweather.data.WeatherRepository
import kotlinx.coroutines.launch

class RecordsViewModel(
    app: Application,
    private val repository: WeatherRepository
) : AndroidViewModel(app) {
    val records = repository.records

    init {
        viewModelScope.launch {
            repository.startRealtimeSync()
        }
    }
}

class RecordsViewModelFactory(
    private val application: Application,
    private val repository: WeatherRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RecordsViewModel(application, repository) as T
    }
}
