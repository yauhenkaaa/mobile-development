package com.example.myweather

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: CityViewModel by viewModels {
        CityViewModelFactory((requireActivity().application as WeatherApp).database.cityDao())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cityNameText = view.findViewById<TextView>(R.id.city_name_text)
        val countryNameText = view.findViewById<TextView>(R.id.country_name_text)
        val temperatureText = view.findViewById<TextView>(R.id.temperature_text)
        val weatherStateText = view.findViewById<TextView>(R.id.weather_state_text)
        val noMainCityText = view.findViewById<TextView>(R.id.no_main_city_text)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mainCity.collect { city ->
                    if (city != null) {
                        cityNameText.text = city.name
                        countryNameText.text = city.country
                        temperatureText.text = "${city.temperature}°C"
                        weatherStateText.text = city.weatherState
                        cityNameText.visibility = View.VISIBLE
                        countryNameText.visibility = View.VISIBLE
                        temperatureText.visibility = View.VISIBLE
                        weatherStateText.visibility = View.VISIBLE
                        noMainCityText.visibility = View.GONE
                    } else {
                        cityNameText.visibility = View.GONE
                        countryNameText.visibility = View.GONE
                        temperatureText.visibility = View.GONE
                        weatherStateText.visibility = View.GONE
                        noMainCityText.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
}