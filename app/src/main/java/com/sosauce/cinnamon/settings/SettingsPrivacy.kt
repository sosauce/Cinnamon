package com.sosauce.cinnamon.settings

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.applock.AppLockTimeout
import com.sosauce.cinnamon.core.applock.PinCreation
import com.sosauce.cinnamon.core.applock.PinVerification
import com.sosauce.cinnamon.core.applock.canUseBiometrics
import com.sosauce.cinnamon.core.ui.components.buttons.CuteNavigationButtonSurface
import com.sosauce.nekobites.components.settings.SelectableTile
import com.sosauce.nekobites.components.settings.SettingsClickable
import com.sosauce.nekobites.components.settings.SettingsGroup
import com.sosauce.nekobites.components.settings.SettingsPicker
import com.sosauce.nekobites.components.settings.SettingsSwitch
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsPrivacy() {
    val viewModel = koinViewModel<AppLockSettingsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsPrivacyContent(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
private fun SettingsPrivacyContent(
    state: AppLockSettingsState,
    onAction: (AppLockSettingsAction) -> Unit
) {
    val context = LocalContext.current
    val canUseBiometrics = remember { context.canUseBiometrics() }
    val timeouts = remember {
        listOf(
            TimeoutOption(AppLockTimeout.IMMEDIATELY, R.string.timeout_short_immediately, R.string.timeout_immediately),
            TimeoutOption(AppLockTimeout.SECONDS_30, R.string.timeout_short_30_seconds, R.string.timeout_30_seconds),
            TimeoutOption(AppLockTimeout.MINUTE_1, R.string.timeout_short_1_minute, R.string.timeout_1_minute),
            TimeoutOption(AppLockTimeout.MINUTES_5, R.string.timeout_short_5_minutes, R.string.timeout_5_minutes),
            TimeoutOption(AppLockTimeout.MINUTES_15, R.string.timeout_short_15_minutes, R.string.timeout_15_minutes)
        )
    }

    SettingsGroup(title = stringResource(R.string.app_lock)) {
        SettingsSwitch(
            title = stringResource(R.string.app_lock_switch),
            description = stringResource(R.string.app_lock_switch_desc),
            checked = state.isEnabled,
            onCheckedChange = { onAction(AppLockSettingsAction.ToggleAppLock(it)) }
        )
        AnimatedVisibility(state.isEnabled) {
            SettingsClickable(
                title = stringResource(R.string.change_pin),
                onClick = { onAction(AppLockSettingsAction.ChangePin) }
            )
        }
        AnimatedVisibility(state.isEnabled && canUseBiometrics) {
            SettingsSwitch(
                title = stringResource(R.string.use_biometrics),
                checked = state.useBiometrics,
                onCheckedChange = { onAction(AppLockSettingsAction.SetUseBiometrics(it)) }
            )
        }
        AnimatedVisibility(state.isEnabled) {
            SettingsPicker(
                title = stringResource(R.string.lock_after)
            ) {
                items(timeouts, key = { it.millis }) { option ->
                    val containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    SelectableTile(
                        selected = state.timeoutMillis == option.millis,
                        onClick = { onAction(AppLockSettingsAction.SetTimeout(option.millis)) },
                        label = stringResource(option.label),
                        width = 88.dp,
                        containerColor = containerColor
                    ) {
                        Text(
                            text = stringResource(option.shortLabel),
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            color = contentColorFor(containerColor)
                        )
                    }
                }
            }
        }
    }

    when (val flow = state.flow) {
        AppLockFlow.CreatePin, AppLockFlow.ChangePin -> FullScreenPinDialog(
            onDismissRequest = { onAction(AppLockSettingsAction.DismissFlow) }
        ) { footer ->
            PinCreation(
                onPinCreated = { onAction(AppLockSettingsAction.PinCreated(it)) },
                footer = { footer() }
            )
        }

        is AppLockFlow.Verify -> FullScreenPinDialog(
            onDismissRequest = { onAction(AppLockSettingsAction.DismissFlow) }
        ) { footer ->
            PinVerification(
                title = stringResource(R.string.verify_its_you),
                description = stringResource(R.string.verify_its_you_desc),
                onVerified = { onAction(AppLockSettingsAction.Verified) },
                // Biometrics are only offered once enabled, so turning them on needs the PIN
                autoPromptKey = flow,
                footer = { footer() }
            )
        }

        null -> Unit
    }
}

/**
 * Full-screen dialog hosting a PIN step, [content] receives the back button to put in its footer.
 */
@Composable
private fun FullScreenPinDialog(
    onDismissRequest: () -> Unit,
    content: @Composable (footer: @Composable () -> Unit) -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            content {
                Row(Modifier.fillMaxWidth()) {
                    CuteNavigationButtonSurface(onNavigateUp = onDismissRequest)
                }
            }
        }
    }
}

private data class TimeoutOption(
    val millis: Long,
    @param:StringRes val shortLabel: Int,
    @param:StringRes val label: Int
)
