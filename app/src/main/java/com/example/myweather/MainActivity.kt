package com.example.myweather

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.myweather.utils.NetworkMonitor
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var networkMonitor: NetworkMonitor
    private var statusJob: Job? = null
    private var firstState = true

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_main)

        checkNotificationPermission()

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        findViewById<BottomNavigationView>(R.id.bottom_nav).setupWithNavController(navController)

        val networkStatusBar = findViewById<TextView>(R.id.network_status_bar)
        networkMonitor = NetworkMonitor(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.isConnected.collectLatest { isConnected ->
                    if (firstState) {
                        firstState = false
                        if (isConnected) return@collectLatest
                    }
                    showNetworkStatus(networkStatusBar, isConnected)
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showNetworkStatus(view: TextView, isConnected: Boolean) {
        statusJob?.cancel()
        statusJob = lifecycleScope.launch {
            if (isConnected) {
                view.text = getString(R.string.back_online)
                view.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.online_blue))
                view.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
            } else {
                view.text = getString(R.string.no_internet)
                view.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.black))
                view.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
            }
            view.visibility = View.VISIBLE
            delay(3000)
            view.visibility = View.GONE
        }
    }
}