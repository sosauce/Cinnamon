package com.sosauce.cinnamon.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.core.applock.AppLockManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Every change to the app lock settings goes through a [AppLockFlow.Verify] step first (PIN, or
 * biometrics when enabled), except turning it on since there's no PIN yet.
 */
class AppLockSettingsViewModel(
    private val appLockManager: AppLockManager
) : ViewModel() {

    private val flow = MutableStateFlow<AppLockFlow?>(null)

    val state: StateFlow<AppLockSettingsState> = combine(
        appLockManager.isEnabled,
        appLockManager.useBiometrics,
        appLockManager.timeoutMillis,
        flow
    ) { isEnabled, useBiometrics, timeoutMillis, flow ->
        AppLockSettingsState(
            isEnabled = isEnabled,
            useBiometrics = useBiometrics,
            timeoutMillis = timeoutMillis,
            flow = flow
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppLockSettingsState(
            isEnabled = appLockManager.isEnabled.value,
            useBiometrics = appLockManager.useBiometrics.value,
            timeoutMillis = appLockManager.timeoutMillis.value
        )
    )

    fun onAction(action: AppLockSettingsAction) {
        when (action) {
            is AppLockSettingsAction.ToggleAppLock -> {
                flow.value = if (action.enabled) {
                    AppLockFlow.CreatePin
                } else {
                    AppLockFlow.Verify(ProtectedChange.Disable)
                }
            }

            AppLockSettingsAction.ChangePin -> {
                flow.value = AppLockFlow.Verify(ProtectedChange.ChangePin)
            }

            is AppLockSettingsAction.SetTimeout -> {
                if (action.timeoutMillis == appLockManager.timeoutMillis.value) return
                flow.value = AppLockFlow.Verify(ProtectedChange.Timeout(action.timeoutMillis))
            }

            is AppLockSettingsAction.SetUseBiometrics -> {
                flow.value = AppLockFlow.Verify(ProtectedChange.Biometrics(action.enabled))
            }

            AppLockSettingsAction.Verified -> onVerified()

            is AppLockSettingsAction.PinCreated -> {
                val current = flow.value
                viewModelScope.launch {
                    when (current) {
                        AppLockFlow.CreatePin -> appLockManager.enable(action.pin)
                        AppLockFlow.ChangePin -> appLockManager.changePin(action.pin)
                        else -> Unit
                    }
                    flow.value = null
                }
            }

            AppLockSettingsAction.DismissFlow -> flow.value = null
        }
    }

    private fun onVerified() {
        val change = (flow.value as? AppLockFlow.Verify)?.change ?: return
        viewModelScope.launch {
            when (change) {
                ProtectedChange.Disable -> {
                    appLockManager.disable()
                    flow.value = null
                }

                ProtectedChange.ChangePin -> flow.value = AppLockFlow.ChangePin

                is ProtectedChange.Timeout -> {
                    appLockManager.setTimeout(change.timeoutMillis)
                    flow.value = null
                }

                is ProtectedChange.Biometrics -> {
                    appLockManager.setUseBiometrics(change.enabled)
                    flow.value = null
                }
            }
        }
    }
}

data class AppLockSettingsState(
    val isEnabled: Boolean = false,
    val useBiometrics: Boolean = false,
    val timeoutMillis: Long = 0L,
    val flow: AppLockFlow? = null
)

/** Full-screen step currently shown above the settings */
sealed interface AppLockFlow {
    data object CreatePin : AppLockFlow
    data object ChangePin : AppLockFlow
    data class Verify(val change: ProtectedChange) : AppLockFlow
}

/** A settings change waiting for the user to be verified */
sealed interface ProtectedChange {
    data object Disable : ProtectedChange
    data object ChangePin : ProtectedChange
    data class Timeout(val timeoutMillis: Long) : ProtectedChange
    data class Biometrics(val enabled: Boolean) : ProtectedChange
}

sealed interface AppLockSettingsAction {
    data class ToggleAppLock(val enabled: Boolean) : AppLockSettingsAction
    data object ChangePin : AppLockSettingsAction
    data class SetTimeout(val timeoutMillis: Long) : AppLockSettingsAction
    data class SetUseBiometrics(val enabled: Boolean) : AppLockSettingsAction
    data object Verified : AppLockSettingsAction
    data class PinCreated(val pin: String) : AppLockSettingsAction
    data object DismissFlow : AppLockSettingsAction
}
