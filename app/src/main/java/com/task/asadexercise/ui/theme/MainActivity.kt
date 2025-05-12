package com.task.asadexercise.ui.theme

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.task.asadexercise.base.CarProgressNotificationHelper
import com.task.asadexercise.viewmodel.SharedVM
import com.task.asadexercise.player.VideoCache
import com.task.asadexercise.base.NavigationGraph
import com.task.asadexercise.screens.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// provide the number of screens to be displayed in the navigation graph.
enum class Destinations { SPLASH, MAIN, DETAIL }

@AndroidEntryPoint
class MainActivity : NavigationGraph<Destinations>() {


    // Define the launcher at class level
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startNotificationSequence()
        } else {
            Toast.makeText(
                this,
                "Notification permission denied",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun startNotificationSequence() {

        notificationHelper = CarProgressNotificationHelper(this)

        lifecycleScope.launch {
            for (progress in 1..10) {
                notificationHelper.showProgressNotification(progress)
                delay(1000) // 1 second delay
            }
        }
    }

    private lateinit var notificationHelper: CarProgressNotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

      checkNotificationPermission()
    }
    override val sharedViewModel: SharedVM by viewModels() // Using SharedViewModel to share data between screens and maintain a consistent state across the app. This allows the ViewModel to persist data and handle communication screens , keep in mind this is only communication purppose every screen should have different viewmodel
    override val startDestination = Destinations.MAIN

    // Defines the mapping of destinations to their corresponding Composable screens
    override val destinations: Map<Destinations, @Composable () -> Unit> =
        mapOf(
            Destinations.SPLASH to {}, // Splash Screen will come here },
            Destinations.MAIN to { MainScreen() },
            Destinations.DETAIL to { }// the detail screen will be implemented here. Generic navigation is already handled, so no need to pass anything in the Composable screen constructor to keep it clean."
        )

    override fun onDestroy() {
        super.onDestroy()
        VideoCache.releaseCache() // Release the cache
    }


    // Then modify your permission check:
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startNotificationSequence()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showPermissionExplanationDialog()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startNotificationSequence()
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Notification Permission Needed")
            .setMessage("This app needs notification permission to show delivery progress updates")
            .setPositiveButton("OK") { _, _ ->
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    CarProgressNotificationHelper.NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}