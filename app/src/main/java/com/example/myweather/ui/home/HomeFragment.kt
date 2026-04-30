package com.example.myweather.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.load
import com.example.myweather.R
import com.example.myweather.WeatherApp
import com.example.myweather.utils.LocationHelper
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val IMAGEKIT_ID = "1qqfgqk0w"
    private val BASE_URL = "https://ik.imagekit.io/$IMAGEKIT_ID/"
    private val TRANSFORMATION = "tr:w-200,h-200"

    private lateinit var locationHelper: LocationHelper
    private lateinit var weatherViewModel: WeatherViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            fetchLocation()
        } else {
            Toast.makeText(requireContext(), R.string.location_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        val app = requireActivity().application as WeatherApp
        if (app.weatherRepository.isSessionExpired()) {
            app.weatherRepository.logout()
            findNavController().navigate(
                R.id.loginFragment,
                null,
                navOptions {
                    popUpTo(R.id.nav_graph) { inclusive = true }
                }
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = requireActivity().application as WeatherApp
        weatherViewModel = ViewModelProvider(
            requireActivity(),
            WeatherViewModelFactory(requireActivity().application, app.weatherRepository)
        )[WeatherViewModel::class.java]
        
        locationHelper = LocationHelper(requireContext())

        val cityNameText = view.findViewById<TextView>(R.id.city_name_text)
        val countryNameText = view.findViewById<TextView>(R.id.country_name_text)
        val temperatureText = view.findViewById<TextView>(R.id.temperature_text)
        val weatherStateText = view.findViewById<TextView>(R.id.weather_state_text)
        val noMainCityText = view.findViewById<TextView>(R.id.no_main_city_text)
        val weatherIcon = view.findViewById<ImageView>(R.id.iv_home_weather_icon)
        val btnLocation = view.findViewById<ImageButton>(R.id.btn_location)
        val locationProgress = view.findViewById<ProgressBar>(R.id.location_progress)

        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.home_swipe_refresh)
        
        btnLocation.setOnClickListener {
            checkPermissionsAndFetchLocation()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                swipeRefresh.setOnRefreshListener {
                    weatherViewModel.refreshNow()
                }

                launch {
                    weatherViewModel.isRefreshing.collect { refreshing ->
                        swipeRefresh.isRefreshing = refreshing
                    }
                }

                launch {
                    weatherViewModel.isLocationLoading.collect { loading ->
                        locationProgress.visibility = if (loading) View.VISIBLE else View.GONE
                        btnLocation.visibility = if (loading) View.INVISIBLE else View.VISIBLE
                    }
                }

                launch {
                    weatherViewModel.cachedWeather.collect { weather ->
                        if (weather == null) {
                            cityNameText.visibility = View.GONE
                            countryNameText.visibility = View.GONE
                            temperatureText.visibility = View.GONE
                            weatherStateText.visibility = View.GONE
                            weatherIcon.visibility = View.GONE
                            noMainCityText.visibility = View.VISIBLE
                            return@collect
                        }

                        val context = requireContext()
                        val cityResId = context.resources.getIdentifier(
                            "city_${weather.cityName.lowercase()}",
                            "string",
                            context.packageName
                        )
                        cityNameText.text = if (cityResId != 0) getString(cityResId) else weather.cityName

                        countryNameText.text = weather.country
                        temperatureText.text = "${weather.temperature}°C"

                        val rawState = weather.weatherState.lowercase()
                        val stateKey = rawState.replace(" ", "_")
                        val stateResId = context.resources.getIdentifier(
                            "weather_$stateKey",
                            "string",
                            context.packageName
                        )
                        weatherStateText.text = if (stateResId != 0) getString(stateResId) else weather.weatherState

                        // Mapping to ImageKit icons
                        val iconName = when {
                            rawState.contains("thunderstorm") -> "thunderstorm"
                            rawState.contains("rain") || rawState.contains("drizzle") -> "rain"
                            rawState.contains("clear") -> "clear"
                            else -> "clouds"
                        }
                        
                        // Correct ImageKit URL format
                        val iconUrl = "${BASE_URL}${TRANSFORMATION}/icons/${iconName}.png"

                        weatherIcon.load(iconUrl) {
                            crossfade(true)
                            placeholder(R.drawable.myweather_logo)
                            error(R.drawable.myweather_logo)
                        }

                        cityNameText.visibility = View.VISIBLE
                        countryNameText.visibility = View.VISIBLE
                        temperatureText.visibility = View.VISIBLE
                        weatherStateText.visibility = View.VISIBLE
                        weatherIcon.visibility = View.VISIBLE
                        noMainCityText.visibility = View.GONE
                    }
                }
                launch {
                    weatherViewModel.errors.collect { messageRes ->
                        Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndFetchLocation() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                fetchLocation()
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun fetchLocation() {
        viewLifecycleOwner.lifecycleScope.launch {
            val location = locationHelper.getCurrentLocation()
            if (location != null) {
                weatherViewModel.updateWeatherByLocation(location.latitude, location.longitude)
            } else {
                Toast.makeText(requireContext(), R.string.location_not_available, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
