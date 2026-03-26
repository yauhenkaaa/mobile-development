package com.example.myweather.ui.details

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.myweather.R
import com.example.myweather.WeatherApp
import com.example.myweather.data.City
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailsFragment : Fragment(R.layout.fragment_details) {

    private var currentCity: City? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as WeatherApp
        val cityNameText = view.findViewById<TextView>(R.id.city_name_text)
        val countryNameText = view.findViewById<TextView>(R.id.country_name_text)
        val temperatureText = view.findViewById<TextView>(R.id.temperature_text)
        val weatherStateText = view.findViewById<TextView>(R.id.weather_state_text)
        val exitButton = view.findViewById<Button>(R.id.exit_button)
        val saveRecordButton = view.findViewById<Button>(R.id.save_record_button)
        val setAsMainButton = view.findViewById<Button>(R.id.set_as_main_button)

        val cityId = arguments?.getInt("cityId") ?: -1
        if (cityId == -1) {
            findNavController().navigateUp()
            return
        }

        val detailsViewModel = ViewModelProvider(
            this,
            DetailsViewModelFactory(cityId, app.database.cityDao(), app.weatherRepository)
        )[DetailsViewModel::class.java]

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                detailsViewModel.city.collect { city ->
                    currentCity = city
                    val context = requireContext()

                    // Localize City Name
                    val cityResId = context.resources.getIdentifier(
                        "city_${city.name.lowercase()}",
                        "string",
                        context.packageName
                    )
                    cityNameText.text = if (cityResId != 0) getString(cityResId) else city.name

                    countryNameText.text = city.country
                    temperatureText.text = "${city.temperature}°C"

                    // Localize Weather State
                    val stateKey = city.weatherState.lowercase().replace(" ", "_")
                    val stateResId = context.resources.getIdentifier(
                        "weather_$stateKey",
                        "string",
                        context.packageName
                    )
                    weatherStateText.text = if (stateResId != 0) getString(stateResId) else city.weatherState

                    setAsMainButton.isEnabled = !city.isMain
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            detailsViewModel.recordSaved.collect {
                Toast.makeText(requireContext(), getString(R.string.record_saved), Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        exitButton.setOnClickListener { findNavController().navigateUp() }

        setAsMainButton.setOnClickListener {
            detailsViewModel.setAsMain()
            Toast.makeText(requireContext(), R.string.record_saved, Toast.LENGTH_SHORT).show()
        }

        saveRecordButton.setOnClickListener {
            currentCity?.let { city ->
                showSaveRecordDialog(city) { cityName, country, temperature, weatherState, recordedAt ->
                    detailsViewModel.saveRecord(
                        cityName = cityName,
                        country = country,
                        temperature = temperature,
                        weatherState = weatherState,
                        recordedAt = recordedAt
                    )
                }
            }
        }
    }

    private fun showSaveRecordDialog(
        city: City,
        onSave: (cityName: String, country: String, temperature: Double, weatherState: String, recordedAt: Long) -> Unit
    ) {
        val contentView = layoutInflater.inflate(R.layout.dialog_save_record, null)
        val cityInput = contentView.findViewById<TextInputEditText>(R.id.record_city_input)
        val countryInput = contentView.findViewById<TextInputEditText>(R.id.record_country_input)
        val temperatureInput = contentView.findViewById<TextInputEditText>(R.id.record_temperature_input)
        val stateInput = contentView.findViewById<TextInputEditText>(R.id.record_state_input)
        val timeInput = contentView.findViewById<TextInputEditText>(R.id.record_time_input)
        val timeLayout = contentView.findViewById<TextInputLayout>(R.id.record_time_layout)

        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        cityInput.setText(city.name)
        countryInput.setText(city.country)
        temperatureInput.setText(city.temperature.toString())
        stateInput.setText(city.weatherState)
        timeInput.setText(formatter.format(Date()))

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.save_record_button)
            .setView(contentView)
            .setNegativeButton(R.string.exit_button, null)
            .setPositiveButton(R.string.save_button, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val recordedAt = runCatching { formatter.parse(timeInput.text?.toString().orEmpty())?.time }.getOrNull()
                        if (recordedAt == null) {
                            timeLayout.error = getString(R.string.invalid_time)
                            return@setOnClickListener
                        }
                        timeLayout.error = null
                        val cityName = cityInput.text?.toString().orEmpty()
                        val country = countryInput.text?.toString().orEmpty()
                        val temperature = temperatureInput.text?.toString()?.toDoubleOrNull() ?: 0.0
                        val weatherState = stateInput.text?.toString().orEmpty()
                        onSave(cityName, country, temperature, weatherState, recordedAt)
                        dialog.dismiss()
                    }
                }
                dialog.show()
            }
    }
}