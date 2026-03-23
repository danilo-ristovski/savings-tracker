package com.savings.tracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.savings.tracker.presentation.main.MainScreen
import com.savings.tracker.presentation.pin.PinLoginScreen
import com.savings.tracker.presentation.pin.PinSetupScreen
import com.savings.tracker.presentation.settings.SettingsScreen
import com.savings.tracker.presentation.trends.TrendsScreen

@Composable
fun NavGraph(startDestination: String) {
    val navController = rememberNavController()

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
