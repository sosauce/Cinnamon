package com.sosauce.cinnamon.activities

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.cinnamon.domain.states.CallState
import com.sosauce.cinnamon.presentation.screens.phone.CallScreen
import com.sosauce.cinnamon.presentation.screens.phone.CallingViewModel
import com.sosauce.cinnamon.presentation.theme.CinnamonTheme
import com.sosauce.cinnamon.utils.APP_PACKAGE
import org.koin.androidx.compose.koinViewModel

class CallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setLockScreenFlags()
        enableEdgeToEdge()
        setContent {
            CinnamonTheme {
                val callViewModel = koinViewModel<CallingViewModel>()
                val callUiState by callViewModel.state.collectAsStateWithLifecycle()

                if (callUiState.callState == CallState.ENDED) { finishAndRemoveTask() }

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
        powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "$APP_PACKAGE:full_wake_lock").acquire(5000L)
    }
}