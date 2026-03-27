package com.savings.tracker.presentation.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.savings.tracker.data.biometric.BiometricHelper
import com.savings.tracker.navigation.Routes

@Composable
fun PinLoginScreen(
    navController: NavController,
    viewModel: PinViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var biometricEnabled by remember { mutableStateOf(false) }
    var showPinFallback by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initForLogin()
        biometricEnabled = viewModel.isBiometricEnabled() && BiometricHelper.isBiometricAvailable(context)
    }

    val triggerBiometric = {
        val activity = context as? FragmentActivity
        if (activity != null) {
            BiometricHelper.authenticate(
                activity = activity,
                onSuccess = { viewModel.onBiometricSuccess() },
                onError = { },
                onNegativeButton = { showPinFallback = true },
            )
        }
    }

    // Trigger biometric automatically if enabled
    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled && !showPinFallback) {
            triggerBiometric()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PinEvent.LoginSuccess -> {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.PIN_LOGIN) { inclusive = true }
                    }
                }
                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (biometricEnabled && !showPinFallback) {
            // Biometric login UI
            Text(
                text = "Biometric Login",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Use your fingerprint or face to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Tap the icon to authenticate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Tap to authenticate",
                modifier = Modifier
                    .size(96.dp)
                    .clickable { triggerBiometric() },
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = { showPinFallback = true }) {
                Text("Use PIN instead")
            }
        } else {
            // PIN login UI
            Text(
                text = "Enter your PIN",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your 6-digit PIN to access your savings",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            PinDots(filledCount = state.pin.length, total = 6)

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLockedOut) {
                val minutes = state.remainingLockoutSeconds / 60
                val seconds = state.remainingLockoutSeconds % 60
                Text(
                    text = "Account locked. Try again in %d:%02d".format(minutes, seconds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            } else if (state.isPostLockout) {
                Text(
                    text = "You have 1 attempt before another lockout",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            state.error?.let { error ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            PinKeypad(
                onDigit = { viewModel.onDigitEntered(it) },
                onBackspace = { viewModel.onBackspace() },
                onClear = { viewModel.onClear() },
                enabled = !state.isLockedOut
            )

            if (biometricEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { showPinFallback = false }) {
                    Text("Use Biometric instead")
                }
            }
        }
    }
}
