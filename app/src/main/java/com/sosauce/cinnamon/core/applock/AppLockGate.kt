package com.sosauce.cinnamon.core.applock

import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.sosauce.cinnamon.core.utils.thenIf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Shows [content] only when the app is unlocked, with [AppLockScreen] above it otherwise.
 *
 * The content isn't composed until the first unlock. After that it stays composed under the lock
 * (hidden from accessibility), so relocking doesn't lose the user's place in the app.
 */
@Composable
fun AppLockGate(
    onExit: () -> Unit,
    content: @Composable () -> Unit
) {
    val appLockManager = koinInject<AppLockManager>()
    val isLocked by appLockManager.isLocked.collectAsStateWithLifecycle()
    var hasBeenUnlocked by remember { mutableStateOf(!isLocked) }
    LaunchedEffect(isLocked) {
        if (!isLocked) hasBeenUnlocked = true
    }

    Box(Modifier.fillMaxSize()) {
        if (hasBeenUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .thenIf(isLocked) {
                        clearAndSetSemantics {}
                    }
            ) {
                content()
            }
        }
        AnimatedVisibility(
            visible = isLocked,
            enter = EnterTransition.None, // NEVER CHANGES THIS, content must NEVER be shown for a split second for the sake of an animation
            exit = fadeOut()
        ) {
            AppLockScreen(onExit = onExit)
        }
    }
}

fun ComponentActivity.bindAppLockSecureFlag(appLockManager: AppLockManager) {}

//fun ComponentActivity.bindAppLockSecureFlag(appLockManager: AppLockManager) {
//    lifecycleScope.launch {
//        appLockManager.isEnabled.collectLatest { enabled ->
//
//            val param = WindowManager.LayoutParams.FLAG_SECURE
//
//            if (enabled) {
//                window.setFlags(param, param)
//            } else {
//                window.clearFlags(param)
//            }
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                setRecentsScreenshotEnabled(false)
//            }
//        }
//    }
//}
