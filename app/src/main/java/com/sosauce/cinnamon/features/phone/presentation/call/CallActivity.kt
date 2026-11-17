package com.sosauce.cinnamon.features.phone.presentation.call

import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.cinnamon.core.ui.CinnamonTheme
import com.sosauce.cinnamon.core.utils.APP_PACKAGE
import com.sosauce.cinnamon.core.utils.createDefaultDialerIntent
import com.sosauce.nekobites.helpers.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

class CallActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLockScreenFlags()
        enableEdgeToEdge()

        setContent {
            val callViewModel = koinViewModel<CallingViewModel>()
            val callUiState by callViewModel.state.collectAsStateWithLifecycle()

            // Asked when the call from the intent is refused because Cinnamon isn't the default dialer app
            val dialerRoleLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val number = intent.data?.toString()?.removePrefix("tel:")

                if (result.resultCode == RESULT_OK && number != null) {
                    callViewModel.handleCallAction(CallAction.LaunchCall(number))
                } else {
                    finishAndRemoveTask()
                }
            }

            ObserveAsEvents(callViewModel.events) { event ->
                when (event) {
                    is CallEvents.RequestDefaultDialer -> try {
                        dialerRoleLauncher.launch(createDefaultDialerIntent())
                    } catch (_: ActivityNotFoundException) {
                        // no dialer role on this device, nothing to call with
                        finishAndRemoveTask()
                    }
                }
            }

            LaunchedEffect(intent) {
                if (intent.action == Intent.ACTION_CALL) {
                    val number = intent.data.toString().removePrefix("tel:")
                    callViewModel.handleCallAction(CallAction.LaunchCall(number))
                }
            }

            LaunchedEffect(callUiState) {
                if (callUiState.callState == CallState.ENDED) {
                    finishAndRemoveTask()
                }
            }
            CinnamonTheme {

                CallScreen(
                    onCallAction = callViewModel::handleCallAction,
                    callUiState = callUiState
                )
            }
        }
    }


    private fun setLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).requestDismissKeyguard(this, null)

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "${APP_PACKAGE}:full_wake_lock")
            .acquire(5000L)
    }
}