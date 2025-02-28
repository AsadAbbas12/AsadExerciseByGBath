package com.task.asadexercise

import androidx.activity.viewModels
import androidx.compose.runtime.Composable

// provide the number of screens to be displayed in the navigation graph.
enum class Destinations { SPLASH, MAIN, DETAIL }
class MainActivity : NavigationGraph<Destinations>() {

    override val sharedViewModel: SharedViewModel by viewModels()
    override val startDestination = Destinations.MAIN

    // Defines the mapping of destinations to their corresponding Composable screens
    override val destinations: Map<Destinations, @Composable () -> Unit> =
        mapOf(
            Destinations.SPLASH to {}, // SHOW  if you want to show },
            Destinations.MAIN to { },
            Destinations.DETAIL to { }
        )
}