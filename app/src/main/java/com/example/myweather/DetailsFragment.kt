package com.example.myweather

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myweather.data.City
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
        val editName = view.findViewById<EditText>(R.id.edit_name)
        val editCountry = view.findViewById<EditText>(R.id.edit_country)
        val editTemp = view.findViewById<EditText>(R.id.edit_temperature)
        val editState = view.findViewById<EditText>(R.id.edit_weather_state)

        val editButton = view.findViewById<Button>(R.id.edit_button)
        val deleteButton = view.findViewById<Button>(R.id.delete_button)
        val saveButton = view.findViewById<Button>(R.id.button_save)

        val cityId = arguments?.getInt("cityId") ?: -1

        if (cityId != -1) {
            lifecycleScope.launch {
                currentCity = viewModel.getCityById(cityId)
                currentCity?.let {
                    cityNameText.text = it.name
                    countryNameText.text = it.country
                    temperatureText.text = "${it.temperature}°C"
                    weatherStateText.text = it.weatherState

                    editName.setText(it.name)
                    editCountry.setText(it.country)
                    editTemp.setText(it.temperature.toString())
                    editState.setText(it.weatherState)
                }
            }
        } else {
            isEditMode = true
            updateUi()
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
                weatherState = state
            )

            if (cityId == -1) {
                viewModel.insertCity(city)
            } else {
                viewModel.updateCity(city)
            }
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

        if (isEditMode) {
            editLayout.visibility = View.VISIBLE
            saveButton.visibility = View.VISIBLE
            editButton.visibility = View.GONE
            deleteButton.visibility = View.GONE

            view.findViewById<TextView>(R.id.city_name_text).visibility = View.GONE
            view.findViewById<TextView>(R.id.country_name_text).visibility = View.GONE
            view.findViewById<TextView>(R.id.temperature_text).visibility = View.GONE
            view.findViewById<TextView>(R.id.weather_state_text).visibility = View.GONE
        } else {
            editLayout.visibility = View.GONE
            saveButton.visibility = View.GONE
            editButton.visibility = View.VISIBLE
            deleteButton.visibility = View.VISIBLE

            view.findViewById<TextView>(R.id.city_name_text).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.country_name_text).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.temperature_text).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.weather_state_text).visibility = View.VISIBLE
        }
    }
}