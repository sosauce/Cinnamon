package com.sosauce.cinnamon.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.sosauce.cinnamon.data.datastore.rememberAppTheme
import com.sosauce.cinnamon.presentation.navigation.Nav
import com.sosauce.cinnamon.presentation.screens.setup.SetupPermissions
import com.sosauce.cinnamon.presentation.theme.CinnamonTheme
import com.sosauce.cinnamon.utils.CuteTheme
import com.sosauce.cinnamon.utils.getAdaptivePrimaryColor
import com.sosauce.cinnamon.utils.hasBothRoles

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        setContent {

            val context = LocalContext.current
            val theme by rememberAppTheme()
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val primary = MaterialTheme.colorScheme.primary
            var seedColor by remember { mutableStateOf(context.getAdaptivePrimaryColor(primary)) }

            CinnamonTheme(seedColor) {

                WindowCompat
                    .getInsetsController(window, window.decorView)
                    .apply {

                        val isLight =
                            if (theme == CuteTheme.SYSTEM) !isSystemInDarkTheme else theme == CuteTheme.LIGHT

                        isAppearanceLightStatusBars = isLight
                        isAppearanceLightNavigationBars = isLight
                    }

                var hasBothRoles by remember { mutableStateOf(hasBothRoles()) }
                if (hasBothRoles) {
                    Nav(
                        intent = intent,
                        onUpdateSeedColor = { seedColor = it }
                    )
                } else {
                    SetupPermissions { hasBothRoles = hasBothRoles() }
                }
            }
        }
    }

}

