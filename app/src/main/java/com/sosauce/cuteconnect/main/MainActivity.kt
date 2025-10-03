package com.sosauce.cuteconnect.main

import android.Manifest
import android.app.role.RoleManager
import android.app.role.RoleManager.ROLE_DIALER
import android.app.role.RoleManager.ROLE_SMS
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.sosauce.cuteconnect.ui.navigation.Nav
import com.sosauce.cuteconnect.ui.screens.setup.SetupScreen
import com.sosauce.cuteconnect.ui.theme.CuteConnectTheme
import com.sosauce.cuteconnect.utils.hasBothRoles

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        setContent {
            CuteConnectTheme {

                WindowCompat
                .getInsetsController(window, window.decorView)
                .apply {
                    isAppearanceLightStatusBars = !isSystemInDarkTheme()
                    isAppearanceLightNavigationBars = !isSystemInDarkTheme()
                }

                var hasBothRoles by remember { mutableStateOf(hasBothRoles()) }
                if (hasBothRoles) {
                    Nav()
                } else {
                    SetupScreen { hasBothRoles = hasBothRoles() }
                }
            }
        }
    }
}

