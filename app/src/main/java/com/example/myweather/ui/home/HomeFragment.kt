package com.example.myweather.ui.home

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myweather.R
import com.example.myweather.WeatherApp
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = requireActivity().application as WeatherApp
        val weatherViewModel = ViewModelProvider(
            requireActivity(),
            WeatherViewModelFactory(requireActivity().application, app.weatherRepository)
        )[WeatherViewModel::class.java]

        val cityNameText = view.findViewById<TextView>(R.id.city_name_text)
        val countryNameText = view.findViewById<TextView>(R.id.country_name_text)
        val temperatureText = view.findViewById<TextView>(R.id.temperature_text)
        val weatherStateText = view.findViewById<TextView>(R.id.weather_state_text)
        val noMainCityText = view.findViewById<TextView>(R.id.no_main_city_text)

        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.home_swipe_refresh)
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
                    weatherViewModel.cachedWeather.collect { weather ->
                        if (weather == null) {
                            cityNameText.visibility = View.GONE
                            countryNameText.visibility = View.GONE
                            temperatureText.visibility = View.GONE
                            weatherStateText.visibility = View.GONE
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

                        val stateKey = weather.weatherState.lowercase().replace(" ", "_")
                        val stateResId = context.resources.getIdentifier(
                            "weather_$stateKey",
                            "string",
                            context.packageName
                        )
                        weatherStateText.text = if (stateResId != 0) getString(stateResId) else weather.weatherState

                        cityNameText.visibility = View.VISIBLE
                        countryNameText.visibility = View.VISIBLE
                        temperatureText.visibility = View.VISIBLE
                        weatherStateText.visibility = View.VISIBLE
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
}