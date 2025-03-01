package com.task.asadexercise

import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun <T : Enum<T>> AppNavigationGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: T,
    actions: NavigationActions<T>,
    destinations: Map<T, @Composable () -> Unit>
) {

    // Provide navigation actions to the composition
    CompositionLocalProvider(LocalNavigationActions provides actions) {
        NavHost(
            navController = navController, startDestination = startDestination.name
        ) {
            destinations.forEach { (destination, content) ->
                composableWithTransitions(
                    route = destination.name,
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Adds activity-like transition animations to a composable screen.
 * The new screen slides in from the right, and the current screen slides out to the left.
 *
 * @param route The route associated with this composable.
 * @param content The composable content to be displayed for this route.
 */
private fun NavGraphBuilder.composableWithTransitions(
    route: String, content: @Composable () -> Unit
) {
// Enter transition: Slide in from the right (positive X direction) and fade in
    val enterTransition = slideInHorizontally(
        initialOffsetX = { it }, animationSpec = tween(durationMillis = 300)
    ) + fadeIn(animationSpec = tween(durationMillis = 300))

// Exit transition: Slide out to the left (negative X direction) and fade out
    val exitTransition = slideOutHorizontally(
        targetOffsetX = { -it }, animationSpec = tween(durationMillis = 300)
    ) + fadeOut(animationSpec = tween(durationMillis = 300))

// Pop enter transition (going back): Slide in from the left and fade in
    val popEnterTransition = slideInHorizontally(
        initialOffsetX = { -it }, animationSpec = tween(durationMillis = 300)
    ) + fadeIn(animationSpec = tween(durationMillis = 300))

// Pop exit transition (going back): Slide out to the right and fade out
    val popExitTransition = slideOutHorizontally(
        targetOffsetX = { it }, animationSpec = tween(durationMillis = 300)
    ) + fadeOut(animationSpec = tween(durationMillis = 300))

    composable(route = route,
        enterTransition = { enterTransition },
        exitTransition = { exitTransition },
        popEnterTransition = { popEnterTransition },
        popExitTransition = { popExitTransition }) {
        content()
    }

}


/**
 * Actions provider to be implemented per navigation flow.
 */
data class NavigationActions<ASAD : Enum<ASAD>>(
    val navController: NavHostController, val screenTo: (ASAD) -> Unit = { destination ->
        navController.navigate(destination.name)
    }, val popBackStack: () -> Unit = {
        navController.popBackStack()
    }, val navigateUp: () -> Unit = {
        navController.navigateUp()
    }
)

/**
 * Local composition provider for navigation actions.
 */
val LocalNavigationActions = compositionLocalOf<NavigationActions<*>> {
    error("Navigation actions are not provided")
}


@Composable
inline fun <reified ASAD : Enum<ASAD>> NavigateToScreen(): NavigationActions<ASAD>? {
    // Attempt to cast the current navigation actions to the specified generic type.
    return LocalNavigationActions.current as? NavigationActions<ASAD>
}


@Composable
inline fun <reified ASAD : ComponentActivity, reified VM : ViewModel> getActivityAndViewModel(): Pair<ASAD, VM> {
    val activity = (LocalContext.current as ASAD)
    val viewModel: VM = ViewModelProvider(activity).get(VM::class.java)
    return Pair(activity, viewModel)
}



