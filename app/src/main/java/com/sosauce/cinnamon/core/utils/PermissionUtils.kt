package com.sosauce.cinnamon.core.utils

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony

object PermissionUtils {

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

}