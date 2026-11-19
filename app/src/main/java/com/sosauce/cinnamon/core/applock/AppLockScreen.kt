package com.sosauce.cinnamon.core.applock

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.cinnamon.R
import org.koin.compose.koinInject

/**
 * Full-screen lock drawn above the app content. It's opaque and swallows touches, the app is
 * unlocked through [AppLockManager] once the PIN/biometric is verified.
 *
 * @param onExit Called on back press, the content behind must not be reachable
 */
@Composable
fun AppLockScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLockManager = koinInject<AppLockManager>()
    val foregroundCount by appLockManager.foregroundCount.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showForgotPin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Don't leave the keyboard of a conversation open above the lock
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    BackHandler(onBack = onExit)

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        PinVerification(
            title = stringResource(R.string.cinnamon_locked),
            description = stringResource(R.string.enter_pin),
            onVerified = {},
            autoPromptKey = foregroundCount,
            footer = {
                TextButton(
                    onClick = { showForgotPin = true },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(
                        text = stringResource(R.string.forgot_pin),
                        style = MaterialTheme.typography.labelLargeEmphasized
                    )
                }
            }
        )
    }

    if (showForgotPin) {
        ForgotPinDialog(onDismissRequest = { showForgotPin = false })
    }
}

@Composable
private fun ForgotPinDialog(onDismissRequest: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                painter = painterResource(R.drawable.lock_filled),
                contentDescription = null
            )
        },
        title = { Text(stringResource(R.string.forgot_pin_title)) },
        text = { Text(stringResource(R.string.forgot_pin_desc)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.open_app_info))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
