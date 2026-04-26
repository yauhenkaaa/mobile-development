package com.example.myweather.data

import android.util.Log
import com.example.myweather.network.OpenWeatherApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class WeatherRepository(
    private val cityDao: CityDao,
    private val cachedWeatherDao: CachedWeatherDao,
    private val weatherRecordDao: WeatherRecordDao,
    private val openWeatherApi: OpenWeatherApi,
    private val apiKey: String
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val belarusCities = listOf(
        "Minsk", "Brest", "Gomel", "Grodno", "Mogilev", "Vitebsk",
        "Bobruisk", "Baranovichi", "Borisov", "Pinsk", "Orsha", "Mozyr",
        "Soligorsk", "Navapolatsk", "Lida", "Molodechno"
    )
    private val _errors = MutableSharedFlow<Int>()
    val errors: SharedFlow<Int> = _errors
    val cachedWeather: Flow<CachedWeather?> = cachedWeatherDao.observeCachedWeather()
    val records: Flow<List<WeatherRecord>> = weatherRecordDao.observeRecords()

    // Auth methods
    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
        migrateLocalRecords()
    }

    suspend fun register(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).await()
        migrateLocalRecords()
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun migrateLocalRecords() {
        val uid = auth.currentUser?.uid ?: return
        weatherRecordDao.updateEmptyUserIds(uid)
    }

    suspend fun ensureDefaultCities() {
        val allCities = cityDao.getAllCitiesSnapshot()
        val canonicalLower = belarusCities.map { it.lowercase() }.toSet()
        
        var mainCityCanonical: String? = null
        val seenNames = mutableSetOf<String>()
        
        allCities.forEach { city ->
            val nameLower = city.name.lowercase()
            if (city.isMain) {
                if (nameLower in canonicalLower) mainCityCanonical = nameLower
                else if (nameLower == "минск" || nameLower == "мінск") mainCityCanonical = "minsk"
            }

            if (nameLower !in canonicalLower || nameLower in seenNames) {
                cityDao.deleteCity(city)
            } else {
                seenNames.add(nameLower)
            }
        }

        val currentNames = cityDao.getAllCitiesSnapshot().map { it.name.lowercase() }.toSet()
        val missingCities = belarusCities.filter { 
            it.lowercase() !in currentNames 
        }.map { name ->
            City(name = name, country = "Belarus", temperature = 0.0, weatherState = "", isMain = false)
        }
        
        if (missingCities.isNotEmpty()) cityDao.insertCities(missingCities)

        if (cityDao.getMainCitySnapshot() == null) {
            val cities = cityDao.getAllCitiesSnapshot()
            val target = cities.find { it.name.lowercase() == (mainCityCanonical ?: "minsk") } ?: cities.firstOrNull()
            target?.let { cityDao.setMainCity(it.id) }
        }
    }

    suspend fun refreshAllCitiesWithRetry(lang: String = Locale.getDefault().language): Boolean {
        ensureDefaultCities()
        val firstTry = refreshAllCitiesFromApi("en")
        if (firstTry) return true
        val secondTry = refreshAllCitiesFromApi("en")
        if (!secondTry) _errors.emit(com.example.myweather.R.string.cannot_update_data)
        return secondTry
    }

    private suspend fun refreshAllCitiesFromApi(lang: String): Boolean {
        if (apiKey.isBlank() || apiKey == "null") return false
        val cities = cityDao.getAllCitiesSnapshot()
        if (cities.isEmpty()) return false

        var successCount = 0
        val refreshedCities = cities.map { city ->
            runCatching {
                val response = openWeatherApi.getCurrentWeather("${city.name},BY", apiKey, lang = lang)
                successCount++
                city.copy(
                    country = response.sys.country,
                    temperature = response.main.temp,
                    weatherState = response.weather.firstOrNull()?.main.orEmpty()
                )
            }.getOrElse { city }
        }
        
        if (refreshedCities.isNotEmpty()) {
            cityDao.insertCities(refreshedCities)
            updateCacheForMainCity()
        }
        return successCount > 0
    }

    suspend fun refreshMainCityByCoords(lat: Double, lon: Double, lang: String = Locale.getDefault().language) {
        try {
            val response = openWeatherApi.getWeatherByCoords(lat, lon, apiKey, lang = lang)
            val cityName = response.name
            
            cachedWeatherDao.upsertCachedWeather(
                CachedWeather(
                    cityId = -1,
                    cityName = cityName,
                    country = response.sys.country,
                    temperature = response.main.temp,
                    weatherState = response.weather.firstOrNull()?.main.orEmpty(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error refreshing weather by coords", e)
            _errors.emit(com.example.myweather.R.string.cannot_update_data)
        }
    }

    private suspend fun updateCacheForMainCity() {
        val city = cityDao.getMainCitySnapshot() ?: return
        cachedWeatherDao.upsertCachedWeather(
            CachedWeather(
                cityId = city.id, cityName = city.name, country = city.country,
                temperature = city.temperature, weatherState = city.weatherState, updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearCacheAndRefresh(isConnected: Boolean, lang: String = Locale.getDefault().language) {
        cachedWeatherDao.clearCache()
        if (isConnected) {
            refreshAllCitiesWithRetry(lang)
        }
    }

    suspend fun addRecordFromCache(): Boolean {
        val cached = cachedWeatherDao.getCachedWeather() ?: return false
        val record = WeatherRecord(
            cityName = cached.cityName,
            country = cached.country,
            temperature = cached.temperature,
            weatherState = cached.weatherState,
            recordedAt = System.currentTimeMillis()
        )
        addRecord(record)
        return true
    }

    suspend fun addRecord(record: WeatherRecord) {
        val uid = auth.currentUser?.uid ?: ""
        val recordWithUser = record.copy(userId = uid)
        val localId = weatherRecordDao.insertRecord(recordWithUser).toInt()
        val recordWithId = recordWithUser.copy(id = localId)
        uploadRecordToFirestore(recordWithId)
    }

    private suspend fun uploadRecordToFirestore(record: WeatherRecord) {
        try {
            val docRef = firestore.collection("weather_records").document()
            val recordToUpload = record.copy(firestoreId = docRef.id)
            docRef.set(recordToUpload).await()
            weatherRecordDao.updateRecord(recordToUpload)
            Log.d("WeatherRepository", "Record uploaded to Firestore with ID: ${docRef.id}")
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error uploading record to Firestore", e)
        }
    }

    fun observeRemoteRecords(): Flow<List<WeatherRecord>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: ""
        if (uid.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val subscription = firestore.collection("weather_records")
            .whereEqualTo("userId", uid)
            .orderBy("recordedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("WeatherRepository", "SnapshotListener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val remoteRecords = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(WeatherRecord::class.java)?.copy(firestoreId = doc.id)
                    }
                    trySend(remoteRecords)
                }
            }
        awaitClose { subscription.remove() }
    }.flowOn(Dispatchers.IO)

    suspend fun startRealtimeSync() {
        withContext(Dispatchers.IO) {
            observeRemoteRecords().collect { remoteRecords ->
                remoteRecords.forEach { remote ->
                    remote.firestoreId?.let { fid ->
                        var local = weatherRecordDao.getRecordByFirestoreId(fid)
                        if (local == null) {
                            local = weatherRecordDao.getRecordByData(remote.cityName, remote.recordedAt)
                        }

                        if (local == null || remote.recordedAt > local.recordedAt) {
                            weatherRecordDao.insertRecord(remote.copy(id = local?.id ?: 0))
                        }
                    }
                }
            }
        }
    }
}
