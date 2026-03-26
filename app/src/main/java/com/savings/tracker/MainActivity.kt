package com.savings.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savings.tracker.data.preferences.PreferencesManager
import com.savings.tracker.domain.model.ThemeMode
import com.savings.tracker.domain.usecase.ApplyMonthlyFeeUseCase
import com.savings.tracker.navigation.NavGraph
import com.savings.tracker.navigation.Routes
import com.savings.tracker.presentation.theme.SavingsTrackingTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    preferencesManager: PreferencesManager,
    private val applyMonthlyFeeUseCase: ApplyMonthlyFeeUseCase,
) : ViewModel() {

    private val _isPinSet = MutableStateFlow<Boolean?>(null)
    val isPinSet: StateFlow<Boolean?> = _isPinSet.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = preferencesManager.themeModeFlow
        .map { name ->
            try { ThemeMode.valueOf(name) } catch (_: Exception) { ThemeMode.SYSTEM }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    init {
        preferencesManager.isPinSetFlow
            .onEach { _isPinSet.value = it }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            applyMonthlyFeeUseCase()
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainActivityViewModel = hiltViewModel()
            val isPinSet by viewModel.isPinSet.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()

            SavingsTrackingTheme(themeMode = themeMode) {

                when (isPinSet) {
                    null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    else -> {
                        val startDestination = if (isPinSet == true) {
                            Routes.PIN_LOGIN
                        } else {
                            Routes.PIN_SETUP
                        }
                        NavGraph(startDestination = startDestination)
                    }
                }
            }
        }
    }
}
