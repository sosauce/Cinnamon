package com.sosauce.cinnamon.core.utils

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

object PermissionUtils {

    typealias DefaultApp = Int
    const val SMS: DefaultApp = 1
    const val DIALER: DefaultApp = 2

    /**
     * Will return new value after setting or not the app as default from the system dialog
     */
    @Composable
    fun observeHasDefaultApp(toCheck: DefaultApp): State<Boolean> {
        val context = LocalContext.current
        val has = remember(toCheck) { mutableStateOf(context.isDefault(toCheck)) }

        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            has.value = context.isDefault(toCheck)
        }

        return has
    }

    private fun Context.isDefault(app: DefaultApp) = when (app) {
        SMS -> isDefaultSmsApp(this)
        else -> isDefaultDialerApp(this)
    }



    fun hasSmsPermission(context: Context): Boolean {
        return context.checkSelfPermission(
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // This should be auto granted when the app is the default dialer, so we never implicitly request it
    fun hasContactsPermission(context: Context): Boolean {
        return context.checkSelfPermission(
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(
                    Manifest.permission.WRITE_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED

    }

    fun hasContactsReadPermission(context: Context): Boolean {
        return context.checkSelfPermission(
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

    }

    fun hasCallLogReadPermission(context: Context): Boolean {
        return context.checkSelfPermission(
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

    }

    fun hasCallLogWritePermission(context: Context): Boolean {
        return context.checkSelfPermission(
            Manifest.permission.WRITE_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

    }

    fun isDefaultSmsApp(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            rm.isRoleHeld(RoleManager.ROLE_SMS)
        } else {
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
    }

    fun isDefaultDialerApp(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            rm.isRoleHeld(RoleManager.ROLE_DIALER)
        } else {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecomManager?.defaultDialerPackage == context.packageName
        }
    }


}