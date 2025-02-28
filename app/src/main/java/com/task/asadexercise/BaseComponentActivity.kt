package com.task.asadexercise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.task.asadexercise.ui.theme.AsadExerciseTheme

abstract class NavigationGraph<D : Enum<D>> : ComponentActivity() {

    abstract val destinations: Map<D, @Composable () -> Unit>
    abstract val startDestination: D
    abstract val sharedViewModel: ViewModel

    @Composable
    open fun Screens(
        navController: NavHostController, actions: NavigationActions<D>
    ) {
        AppNavigationGraph(
            navController = navController,
            startDestination = startDestination,
            actions = actions,
            destinations = destinations
        )
    }

    /**
     * Initialize the activity and set up the content view
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeArguments()

        setContent {
            val isDarkMode = isSystemInDarkTheme()
            AsadExerciseTheme(darkTheme = isDarkMode) {
                CompositionLocalProvider {
                    Surface(
                        color = colorResource(id = R.color.purple_500),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val navController = rememberNavController()
                        val actions = remember { NavigationActions<D>(navController) }
                        DisplayContent(navController, actions = actions)
                    }
                }
            }
        }
    }

    /**
     * Abstract function to define the composable content to be displayed.
     */
    @Composable
    fun DisplayContent(
        navController: NavHostController, actions: NavigationActions<D>
    ) = Screens(navController = navController, actions = actions)

    /**
     * Open function to initialize arguments. Can be overridden by subclasses.
     */
    open fun initializeArguments() {

    }
}


