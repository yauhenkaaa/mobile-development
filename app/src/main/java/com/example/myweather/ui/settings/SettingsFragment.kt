package com.example.myweather.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.myweather.R
import com.example.myweather.WeatherApp

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var viewModel: SettingsViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val app = requireActivity().application as WeatherApp
        viewModel = ViewModelProvider(
            this,
            SettingsViewModelFactory(requireActivity().application, app.weatherRepository)
        )[SettingsViewModel::class.java]

        setupAccountPreferences()

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

    private fun setupAccountPreferences() {
        findPreference<Preference>("pref_user_email")?.summary = viewModel.currentUserEmail ?: "—"
        
        findPreference<Preference>("pref_logout")?.setOnPreferenceClickListener {
            viewModel.logout()
            findNavController().navigate(
                R.id.loginFragment,
                null,
                navOptions {
                    popUpTo(R.id.nav_graph) { inclusive = true }
                }
            )
            true
        }
    }

    private fun showClearCacheConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.clear_cache_title))
            .setMessage(getString(R.string.clear_cache_confirm_message))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.clearCache()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}