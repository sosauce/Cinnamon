package com.sosauce.cinnamon.app

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.sosauce.cinnamon.app.navigation.Nav
import com.sosauce.cinnamon.core.applock.AppLockGate
import com.sosauce.cinnamon.core.applock.AppLockManager
import com.sosauce.cinnamon.core.applock.bindAppLockSecureFlag
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.core.datastore.rememberLastCrash
import com.sosauce.cinnamon.core.ui.CinnamonTheme
import com.sosauce.cinnamon.core.ui.CrashScreen
import com.sosauce.cinnamon.core.utils.PermissionUtils
import com.sosauce.cinnamon.setup.SetupScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val userPreferences by inject<UserPreferences>()
    private val appLockManager by inject<AppLockManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        bindAppLockSecureFlag(appLockManager)


        setContent {
            CinnamonTheme {
                AppLockGate(
                    onExit = { moveTaskToBack(true) }
                ) {
                    // Setup is only shown once: the roles can be skipped there, the screens needing
                    // them ask again later
                    var showSetup by remember {
                        mutableStateOf(
                            !PermissionUtils.isDefaultSmsApp(this) &&
                                    !runBlocking { userPreferences.setupCompleted.first() }
                        )
                    }
                    var lastCrash by rememberLastCrash()

                    when {
                        lastCrash.isNotEmpty() -> CrashScreen(
                            crashLog = lastCrash,
                            onClose = { lastCrash = "" }
                        )

                        showSetup -> SetupScreen {
                            lifecycleScope.launch { userPreferences.saveSetupCompleted() }
                            showSetup = false
                        }

                        else -> Nav(intent = intent)
                    }
                }
            }
        }
    }

    override fun onStart() {
        // Before super so the lock state is already right when Compose resumes
        appLockManager.onActivityStarted()
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        appLockManager.onActivityResumed()
    }

    override fun onStop() {
        super.onStop()
        appLockManager.onActivityStopped(isChangingConfigurations)
    }

    // Every ActivityResultLauncher (pickers, backups, default-app roles...) ends up in these two
    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        appLockManager.onLaunchingForResult()
        super.startActivityForResult(intent, requestCode, options)
    }

    override fun startIntentSenderForResult(
        intent: IntentSender,
        requestCode: Int,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int,
        options: Bundle?
    ) {
        appLockManager.onLaunchingForResult()
        super.startIntentSenderForResult(
            intent,
            requestCode,
            fillInIntent,
            flagsMask,
            flagsValues,
            extraFlags,
            options
        )
    }

}
