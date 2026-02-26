package com.example.myweather

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.myweather.data.City
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class DetailsFragment : Fragment(R.layout.fragment_details) {

    private val viewModel: CityViewModel by viewModels {
        CityViewModelFactory((requireActivity().application as WeatherApp).database.cityDao())
    }

    private var currentCity: City? = null
    private var isEditMode = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cityNameText = view.findViewById<TextView>(R.id.city_name_text)
        val countryNameText = view.findViewById<TextView>(R.id.country_name_text)
        val temperatureText = view.findViewById<TextView>(R.id.temperature_text)
        val weatherStateText = view.findViewById<TextView>(R.id.weather_state_text)

        val editLayout = view.findViewById<LinearLayout>(R.id.edit_layout)
        val editName = view.findViewById<TextInputEditText>(R.id.edit_name)
        val editCountry = view.findViewById<TextInputEditText>(R.id.edit_country)
        val editTemp = view.findViewById<TextInputEditText>(R.id.edit_temperature)
        val editState = view.findViewById<TextInputEditText>(R.id.edit_weather_state)

        val setMainButton = view.findViewById<Button>(R.id.set_main_button)
        val editButton = view.findViewById<Button>(R.id.edit_button)
        val deleteButton = view.findViewById<Button>(R.id.delete_button)
        val saveButton = view.findViewById<Button>(R.id.button_save)

        val cityId = arguments?.getInt("cityId") ?: -1

        if (cityId != -1) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.getCityById(cityId)?.let { city ->
                        currentCity = city
                        cityNameText.text = city.name
                        countryNameText.text = city.country
                        temperatureText.text = "${city.temperature}°C"
                        weatherStateText.text = city.weatherState

                        editName.setText(city.name)
                        editCountry.setText(city.country)
                        editTemp.setText(city.temperature.toString())
                        editState.setText(city.weatherState)

                        viewModel.mainCity.collect { mainCity ->
                            setMainButton.visibility = if (city.id == mainCity?.id) View.GONE else View.VISIBLE
                        }
                    }
                }
            }
        } else {
            isEditMode = true
            updateUi()
        }

        setMainButton.setOnClickListener {
            currentCity?.let {
                viewModel.setMainCity(it)
                Toast.makeText(requireContext(), "'${it.name}' установлен как главный город", Toast.LENGTH_SHORT).show()
            }
        }

        editButton.setOnClickListener {
            isEditMode = !isEditMode
            updateUi()
        }

        deleteButton.setOnClickListener {
            currentCity?.let {
                viewModel.deleteCity(it)
                findNavController().navigateUp()
            }
        }

        saveButton.setOnClickListener {
            val name = editName.text.toString()
            val country = editCountry.text.toString()
            val temp = editTemp.text.toString().toDoubleOrNull() ?: 0.0
            val state = editState.text.toString()

            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Введите название города", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val city = City(
                id = if (cityId == -1) 0 else cityId,
                name = name,
                country = country,
                temperature = temp,
                weatherState = state,
                isMain = currentCity?.isMain ?: false
            )

            if (cityId == -1) {
                viewModel.insertCity(city)
            } else {
                viewModel.updateCity(city)
            }
            isEditMode = false
            findNavController().navigateUp()
        }

        updateUi()
    }

    private fun updateUi() {
        val view = requireView()
        val editLayout = view.findViewById<LinearLayout>(R.id.edit_layout)
        val saveButton = view.findViewById<Button>(R.id.button_save)
        val editButton = view.findViewById<Button>(R.id.edit_button)
        val deleteButton = view.findViewById<Button>(R.id.delete_button)
        val setMainButton = view.findViewById<Button>(R.id.set_main_button)

        val viewModeVisibility = if (isEditMode) View.GONE else View.VISIBLE
        val editModeVisibility = if (isEditMode) View.VISIBLE else View.GONE

        editLayout.visibility = editModeVisibility
        saveButton.visibility = editModeVisibility

        editButton.visibility = viewModeVisibility
        deleteButton.visibility = viewModeVisibility
        setMainButton.visibility = viewModeVisibility

        view.findViewById<TextView>(R.id.city_name_text).visibility = viewModeVisibility
        view.findViewById<TextView>(R.id.country_name_text).visibility = viewModeVisibility
        view.findViewById<TextView>(R.id.temperature_text).visibility = viewModeVisibility
        view.findViewById<TextView>(R.id.weather_state_text).visibility = viewModeVisibility
    }
}