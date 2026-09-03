package com.sosauce.cinnamon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.sosauce.cinnamon.core.datastore.rememberAppTheme
import com.sosauce.cinnamon.app.navigation.Nav
import com.sosauce.cinnamon.setup.SetupScreen
import com.sosauce.cinnamon.core.ui.CinnamonTheme
import com.sosauce.cinnamon.core.utils.CuteTheme
import android.app.role.RoleManager
import android.os.Build
import android.provider.Telephony
import com.sosauce.cinnamon.core.utils.hasBothRoles

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        setContent {
            CinnamonTheme {

                // Dialer role no longer required — always use Cinnamon's call UI (CallScreen/Incoming UI)
                // Only gate on SMS default (needed for messaging). hasBothRoles kept for legacy but dialer part ignored.
                var hasSmsRole by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val rm = getSystemService(RoleManager::class.java)
                            rm.isRoleHeld(RoleManager.ROLE_SMS)
                        } else {
                            Telephony.Sms.getDefaultSmsPackage(this@MainActivity) == packageName
                        }
                    )
                }
                if (hasSmsRole) {
                    Nav(
                        intent = intent
                    )
                } else {
                    SetupScreen { hasSmsRole = true }
                }
            }
        }
    }

}

