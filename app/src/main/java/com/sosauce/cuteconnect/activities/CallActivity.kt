package com.sosauce.cuteconnect.activities

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.cuteconnect.ui.screens.phone.CallScreen
import com.sosauce.cuteconnect.ui.theme.CuteConnectTheme
import com.sosauce.cuteconnect.viewModels.CallViewModel
import androidx.compose.runtime.getValue
import com.sosauce.cuteconnect.domain.states.CallState
import com.sosauce.cuteconnect.ui.screens.phone.IncomingScreen
import com.sosauce.cuteconnect.utils.APP_PACKAGE
import org.koin.androidx.compose.koinViewModel

class CallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setLockScreenFlags()
        enableEdgeToEdge()
        setContent {
            CuteConnectTheme {
                val callViewModel = koinViewModel<CallViewModel>()
                val callUiState by callViewModel.callUiState.collectAsStateWithLifecycle()


                if (callUiState.callState == CallState.ENDED) { finish() }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { _ ->

                    if (callUiState.callState == CallState.RINGING) {
                        IncomingScreen(
                            onCallActions = callViewModel::onHandleCallAction,
                            callUiState = callUiState
                        )
                    } else {
                        CallScreen(
                            onCallAction = callViewModel::onHandleCallAction,
                            callUiState = callUiState
                        )
                    }

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
        powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "$APP_PACKAGE:full_wake_lock").acquire(5000L)
    }
}