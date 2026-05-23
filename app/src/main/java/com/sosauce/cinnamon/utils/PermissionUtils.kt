package com.sosauce.cinnamon.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

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

}