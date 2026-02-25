package com.example.myweather

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat

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
    }
}