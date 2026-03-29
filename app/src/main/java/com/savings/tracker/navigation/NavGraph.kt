package com.savings.tracker.navigation

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.savings.tracker.data.preferences.PreferencesManager
import com.savings.tracker.data.sensor.ShakeDetector
import com.savings.tracker.presentation.balance.MonthlyBalanceScreen
import com.savings.tracker.presentation.main.MainScreen
import com.savings.tracker.presentation.pin.PinLoginScreen
import com.savings.tracker.presentation.pin.PinSetupScreen
import com.savings.tracker.presentation.settings.SettingsScreen
import com.savings.tracker.presentation.trends.TrendsScreen
import kotlinx.coroutines.delay

private const val INACTIVITY_TIMEOUT_MS = 60_000L

@Composable
fun NavGraph(startDestination: String, preferencesManager: PreferencesManager) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // On app resume from background: immediately check if timeout already elapsed
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val route = navController.currentDestination?.route
                if (route != Routes.PIN_LOGIN && route != Routes.PIN_SETUP) {
                    val elapsed = System.currentTimeMillis() - preferencesManager.lastInteractionTime
                    if (elapsed >= INACTIVITY_TIMEOUT_MS) {
                        navController.navigate(Routes.PIN_LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Polling loop for inactivity while app is in foreground
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            if (currentRoute != Routes.PIN_LOGIN && currentRoute != Routes.PIN_SETUP) {
                if (System.currentTimeMillis() - preferencesManager.lastInteractionTime >= INACTIVITY_TIMEOUT_MS) {
                    navController.navigate(Routes.PIN_LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                    break
                }
            }
        }
    }

    // Shake-to-logout
    val shakeEnabled by preferencesManager.shakeLogoutEnabledFlow.collectAsState(initial = false)

    DisposableEffect(shakeEnabled, currentRoute) {
        if (!shakeEnabled || currentRoute == Routes.PIN_LOGIN || currentRoute == Routes.PIN_SETUP) {
            return@DisposableEffect onDispose { }
        }

        val sensorManager = context.getSystemService<SensorManager>()
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val mainHandler = Handler(Looper.getMainLooper())
        val shakeDetector = ShakeDetector {
            mainHandler.post {
                navController.navigate(Routes.PIN_LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(shakeDetector)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        preferencesManager.lastInteractionTime = System.currentTimeMillis()
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

            composable(Routes.MONTHLY_BALANCE) {
                MonthlyBalanceScreen(navController = navController)
            }
        }
    }
}
