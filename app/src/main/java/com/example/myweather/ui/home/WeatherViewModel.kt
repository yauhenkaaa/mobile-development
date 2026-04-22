package com.example.myweather.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myweather.utils.NetworkMonitor
import com.example.myweather.data.WeatherRepository
import com.example.myweather.notifications.WeatherNotificationWorker
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WeatherViewModel(
    private val app: Application,
    private val repository: WeatherRepository
) : AndroidViewModel(app) {

    private val networkMonitor = NetworkMonitor(app.applicationContext)
    private val refreshMutex = Mutex()

    val cachedWeather = repository.cachedWeather
    val errors = repository.errors
    val isRefreshing: Flow<Boolean> get() = _isRefreshing

    private val _isRefreshing = MutableStateFlow(false)

    private var tickerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureDefaultCities()
            networkMonitor.isConnected.collectLatest { isConnected ->
                if (!isConnected) {
                    tickerJob?.cancel()
                    tickerJob = null
                    return@collectLatest
                }

                if (tickerJob == null) {
                    tickerJob = launch {
                        refreshAllSafe()
                        while (isActive) {
                            delay(60_000L)
                            refreshAllSafe()
                        }
                    }
                }
            }
        }
    }

    fun refreshNow() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val success = repository.refreshAllCitiesWithRetry()
                if (success) {
                    WeatherNotificationWorker.triggerNow(app)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    suspend fun refreshNowSuspend(): Boolean {
        return refreshAllSafe()
    }

    private suspend fun refreshAllSafe(): Boolean {
        _isRefreshing.value = true
        return refreshMutex.withLock {
            try {
                repository.refreshAllCitiesWithRetry()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

class WeatherViewModelFactory(
    private val application: Application,
    private val repository: WeatherRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WeatherViewModel(application, repository) as T
    }
}