package com.sosauce.cinnamon.app

import android.app.role.RoleManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sosauce.cinnamon.app.navigation.Nav
import com.sosauce.cinnamon.core.datastore.rememberLastCrash
import com.sosauce.cinnamon.core.ui.CinnamonTheme
import com.sosauce.cinnamon.core.ui.CrashScreen
import com.sosauce.cinnamon.core.utils.PermissionUtils
import com.sosauce.cinnamon.setup.SetupScreen

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()





        setContent {
            CinnamonTheme {
                var hasSmsRole by remember { mutableStateOf(PermissionUtils.isDefaultSmsApp(this)) }
                var lastCrash by rememberLastCrash()

                when {
                    lastCrash.isNotEmpty() -> CrashScreen(
                        crashLog = lastCrash,
                        onClose = { lastCrash = "" }
                    )
                    hasSmsRole -> Nav(intent = intent)
                    else -> SetupScreen { hasSmsRole = true }
                }

            }
        }
    }

}

