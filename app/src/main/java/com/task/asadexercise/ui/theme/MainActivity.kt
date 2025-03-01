package com.task.asadexercise.ui.theme

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.task.asadexercise.viewmodel.SharedVM
import com.task.asadexercise.player.VideoCache
import com.task.asadexercise.base.NavigationGraph
import com.task.asadexercise.screens.MainScreen
import dagger.hilt.android.AndroidEntryPoint

// provide the number of screens to be displayed in the navigation graph.
enum class Destinations { SPLASH, MAIN, DETAIL }

@AndroidEntryPoint
class MainActivity : NavigationGraph<Destinations>() {

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
}