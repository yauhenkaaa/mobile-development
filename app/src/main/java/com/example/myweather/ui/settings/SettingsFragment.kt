package com.example.myweather.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.myweather.R
import com.example.myweather.WeatherApp

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<ListPreference>("language_list")?.setOnPreferenceChangeListener { _, newValue ->
            val locale = LocaleListCompat.forLanguageTags(newValue.toString())
            AppCompatDelegate.setApplicationLocales(locale)
            true
        }

        findPreference<ListPreference>("theme_list")?.setOnPreferenceChangeListener { _, newValue ->
            val mode = when (newValue.toString()) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)
            true
        }

        findPreference<Preference>("clear_cache")?.setOnPreferenceClickListener {
            showClearCacheConfirmation()
            true
        }
    }

    private fun showClearCacheConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.clear_cache_title))
            .setMessage(getString(R.string.clear_cache_confirm_message))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val app = requireActivity().application as WeatherApp
                val settingsViewModel = ViewModelProvider(
                    this,
                    SettingsViewModelFactory(requireActivity().application, app.weatherRepository)
                )[SettingsViewModel::class.java]
                settingsViewModel.clearCache()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}