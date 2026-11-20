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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sosauce.cinnamon.core.ui.CinnamonTheme
import com.sosauce.cinnamon.core.ui.components.dialogs.SimPickerDialog
import com.sosauce.cinnamon.core.utils.APP_PACKAGE
import com.sosauce.cinnamon.core.utils.createDefaultDialerIntent
import com.sosauce.nekobites.helpers.ObserveAsEvents
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class CallActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLockScreenFlags()
        enableEdgeToEdge()

        setContent {
            val callViewModel = koinViewModel<CallingViewModel>()
            val callUiState by callViewModel.state.collectAsStateWithLifecycle()
            val simPicker by callViewModel.simPicker.collectAsStateWithLifecycle()

            // The calling state is still ENDED from the previous call until the call from the intent
            // is launched, don't finish meanwhile or the SIM picker would never show up
            var awaitingIntentCall by remember { mutableStateOf(intent.action == Intent.ACTION_CALL) }
            LaunchedEffect(callUiState.callState, simPicker) {
                if (callUiState.callState != CallState.ENDED || simPicker != null) {
                    awaitingIntentCall = false
                }
            }

            val dialerRoleLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val number = intent.data?.schemeSpecificPart

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
                        // didn't get the dialer role, too bad!
                        finishAndRemoveTask()
                    }
                }
            }

            LaunchedEffect(intent) {
                if (intent.action == Intent.ACTION_CALL) {
                    val number = intent.data?.schemeSpecificPart
                    if (number != null) {
                        callViewModel.handleCallAction(CallAction.LaunchCall(number))
                    } else {
                        finishAndRemoveTask()
                    }
                }
            }

            LaunchedEffect(callUiState, simPicker, awaitingIntentCall) {
                if (callUiState.callState == CallState.ENDED && simPicker == null && !awaitingIntentCall) {
                    finishAndRemoveTask()
                }
            }



            CinnamonTheme {

                simPicker?.let { picker ->
                    SimPickerDialog(
                        state = picker,
                        onPickSim = { callViewModel.handleCallAction(CallAction.PickSim(it)) },
                        onDismissRequest = {
                            callViewModel.handleCallAction(CallAction.DismissSimPicker)
                            finishAndRemoveTask()
                        }
                    )
                }

                if (simPicker == null && !awaitingIntentCall) {
                    CallScreen(
                        onCallAction = callViewModel::handleCallAction,
                        callUiState = callUiState
                    )
                }
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