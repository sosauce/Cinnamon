package com.sosauce.cinnamon.core.system.services

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.cinnamon.core.telephony.phone.CallManager
import com.sosauce.cinnamon.core.ui.CinnamonTheme
import com.sosauce.cinnamon.features.phone.presentation.call.CallAction
import com.sosauce.cinnamon.features.phone.presentation.call.CallActivity
import com.sosauce.cinnamon.features.phone.presentation.call.CallState
import com.sosauce.cinnamon.features.phone.presentation.call.components.CallBubble
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Manages system overlay bubble shown over other apps during a call.
 * M3 Expressive blur background via tonal surface + WindowManager TYPE_APPLICATION_OVERLAY
 */
class CallOverlayManager(
    private val context: Context,
    private val callManager: CallManager
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var isShowing = false

    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

    fun requestOverlayPermission() {
        if (!canDrawOverlays()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        }
    }

    fun observe(scope: CoroutineScope, onToggleMute: () -> Unit = {}, onEndCall: () -> Unit = {}) {
        // Combine call state + CallActivity visibility so bubble reacts to both
        kotlinx.coroutines.flow.combine(
            callManager.callingState,
            com.sosauce.cinnamon.app.CinnamonApplication.isCallActivityVisibleFlow
        ) { state, isCallUiVisible -> state to isCallUiVisible }
            .onEach { (state, isCallUiVisible) ->
                when (state.callState) {
                    CallState.RINGING, CallState.DIALING, CallState.ONGOING -> {
                        if (!isCallUiVisible && !isShowing && canDrawOverlays()) {
                            showOverlay(onToggleMute, onEndCall)
                        } else if (isCallUiVisible && isShowing) {
                            hideOverlay()
                        } else if (!isCallUiVisible && isShowing) {
                            // Already showing, ensure stays (no-op)
                        } else if (!isCallUiVisible && !canDrawOverlays() && !isShowing) {
                            // Permission not granted — don't crash, will show when granted
                        }
                    }
                    CallState.ENDED -> hideOverlay()
                    else -> {
                        if (isShowing) hideOverlay()
                    }
                }
            }
            .launchIn(scope)
    }

    fun showOverlay(
        onToggleMute: () -> Unit = {},
        onEndCall: () -> Unit = {}
    ) {
        if (isShowing || !canDrawOverlays()) return
        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 24 // below status bar, 8dp system
            }

            composeView = ComposeView(context).apply {
                setContent {
                    CinnamonTheme {
                        OverlayContent(onToggleMute, onEndCall)
                    }
                }
            }
            windowManager.addView(composeView, params)
            isShowing = true
        } catch (e: Exception) {
            android.util.Log.e("CallOverlay", "showOverlay failed", e)
        }
    }

    fun hideOverlay() {
        if (!isShowing) return
        try {
            composeView?.let { windowManager.removeView(it) }
        } catch (_: Exception) {}
        composeView = null
        isShowing = false
    }

    @Composable
    private fun OverlayContent(
        onToggleMute: () -> Unit,
        onEndCall: () -> Unit
    ) {
        val state by callManager.callingState.collectAsStateWithLifecycle()
        CallBubble(
            state = state,
            onExpand = {
                // Tap bubble to open full CallActivity
                val intent = Intent(context, CallActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(intent)
                // Optionally hide overlay when expanding to full UI
                // hideOverlay()
            },
            onEndCall = {
                onEndCall()
                callManager.hangupOngoingCall()
                // Also force ENDED for immediate hide
                hideOverlay()
            },
            onToggleMute = {
                onToggleMute()
                callManager.toggleMute(!state.isMuted)
            },
            onToggleSpeaker = {
                // Handled via audio route — toggle speaker for demo
                val current = state.currentAudioRoute.type
                val next = if (current == android.telecom.CallAudioState.ROUTE_SPEAKER)
                    android.telecom.CallAudioState.ROUTE_EARPIECE else android.telecom.CallAudioState.ROUTE_SPEAKER
                callManager.switchAudioRoute(
                    com.sosauce.cinnamon.features.phone.domain.AudioRoute(
                        name = if (next == android.telecom.CallAudioState.ROUTE_SPEAKER) "Speaker" else "Earpiece",
                        type = next
                    )
                )
            }
        )
    }
}
