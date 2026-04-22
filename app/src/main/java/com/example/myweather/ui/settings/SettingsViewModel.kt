package com.example.myweather.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myweather.data.WeatherRepository
import com.example.myweather.utils.NetworkMonitor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    app: Application,
    private val repository: WeatherRepository
) : AndroidViewModel(app) {

    private val networkMonitor = NetworkMonitor(app.applicationContext)

    fun clearCache() {
        viewModelScope.launch {
            val isConnected = networkMonitor.isConnected.first()
            repository.clearCacheAndRefresh(isConnected)
        }
    }
}

class SettingsViewModelFactory(
    private val application: Application,
    private val repository: WeatherRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(application, repository) as T
    }
}

