package com.savings.tracker.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.savings.tracker.presentation.main.MainScreen
import com.savings.tracker.presentation.pin.PinLoginScreen
import com.savings.tracker.presentation.pin.PinSetupScreen
import com.savings.tracker.presentation.settings.SettingsScreen
import com.savings.tracker.presentation.trends.TrendsScreen
import kotlinx.coroutines.delay

private const val INACTIVITY_TIMEOUT_MS = 180_000L

@Composable
fun NavGraph(startDestination: String) {
    val navController = rememberNavController()
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Auto-logout after inactivity
    LaunchedEffect(lastInteractionTime) {
        if (currentRoute != Routes.PIN_LOGIN && currentRoute != Routes.PIN_SETUP) {
            delay(INACTIVITY_TIMEOUT_MS)
            navController.navigate(Routes.PIN_LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        lastInteractionTime = System.currentTimeMillis()
                    }
                }
            }
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Routes.PIN_SETUP) {
                PinSetupScreen(navController = navController)
            }

            composable(Routes.PIN_LOGIN) {
                PinLoginScreen(navController = navController)
            }

            composable(Routes.MAIN) {
                MainScreen(navController = navController)
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(navController = navController)
            }

            composable(Routes.TRENDS) {
                TrendsScreen(navController = navController)
            }
        }
    }
}
