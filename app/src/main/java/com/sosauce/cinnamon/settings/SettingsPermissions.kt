@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.settings

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.createDefaultDialerIntent
import com.sosauce.cinnamon.core.utils.createDefaultSmsIntent
import com.sosauce.nekobites.components.settings.SettingsGroup
import com.sosauce.nekobites.components.settings.SettingsIconBadge
import com.sosauce.nekobites.components.settings.SettingsItem
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun SettingsPermissions() {
    val context = LocalContext.current
    val telecomManager = remember { context.getSystemService(TelecomManager::class.java) }
    val hazeState = remember { HazeState() }

    var isDefaultDialer by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rm = context.getSystemService(RoleManager::class.java)
                rm.isRoleHeld(RoleManager.ROLE_DIALER)
            } else {
                telecomManager?.defaultDialerPackage == context.packageName
            }
        )
    }
    var isDefaultSms by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rm = context.getSystemService(RoleManager::class.java)
                rm.isRoleHeld(RoleManager.ROLE_SMS)
            } else {
                Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
            }
        )
    }

    val dialerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultDialer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            rm.isRoleHeld(RoleManager.ROLE_DIALER)
        } else {
            telecomManager?.defaultDialerPackage == context.packageName
        }
    }
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultSms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            rm.isRoleHeld(RoleManager.ROLE_SMS)
        } else {
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
    }

    Column(
        modifier = Modifier.hazeSource(state = hazeState)
    ) {
        SettingsGroup(title = stringResource(R.string.default_apps)) {
            DefaultAppItem(
                icon = R.drawable.phone_filled,
                title = stringResource(R.string.default_dialer_app),
                description = stringResource(R.string.default_dialer_desc),
                isDefault = isDefaultDialer,
                defaultLabel = stringResource(R.string.is_default_dialer),
                actionLabel = stringResource(R.string.set_as_default_dialer),
                container = MaterialTheme.colorScheme.primaryContainer,
                onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                action = MaterialTheme.colorScheme.primary,
                onAction = MaterialTheme.colorScheme.onPrimary,
                badgeShape = MaterialShapes.Cookie9Sided.toShape(),
                onClick = {
                    val roleIntent = context.createDefaultDialerIntent()
                    if (roleIntent.action != null && roleIntent.action!!.isNotEmpty()) {
                        try {
                            dialerLauncher.launch(roleIntent)
                        } catch (_: Exception) {
                            try { context.startActivity(roleIntent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                        }
                    } else {
                        val fallback = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                            putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                        }
                        try {
                            dialerLauncher.launch(fallback)
                        } catch (_: Exception) {
                            try { context.startActivity(fallback.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                        }
                    }
                }
            )
            DefaultAppItem(
                icon = R.drawable.messages_filled,
                title = stringResource(R.string.default_sms_app),
                description = stringResource(R.string.default_sms_desc),
                isDefault = isDefaultSms,
                defaultLabel = stringResource(R.string.is_default_sms),
                actionLabel = stringResource(R.string.set_as_sms_app),
                container = MaterialTheme.colorScheme.tertiaryContainer,
                onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                action = MaterialTheme.colorScheme.tertiary,
                onAction = MaterialTheme.colorScheme.onTertiary,
                badgeShape = MaterialShapes.Clover4Leaf.toShape(),
                onClick = {
                    val intent = context.createDefaultSmsIntent()
                    if (intent.action?.isNotEmpty() == true) {
                        smsLauncher.launch(intent)
                    }
                }
            )
        }
        Text(
            text = stringResource(R.string.bubbles_tip),
            style = MaterialTheme.typography.bodySmallEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        )
    }
}

@Composable
private fun DefaultAppItem(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    isDefault: Boolean,
    defaultLabel: String,
    actionLabel: String,
    container: Color,
    onContainer: Color,
    action: Color,
    onAction: Color,
    badgeShape: Shape,
    onClick: () -> Unit
) {
    val badgeContainer by animateColorAsState(
        if (isDefault) container else MaterialTheme.colorScheme.surfaceContainerHighest
    )
    val badgeContent by animateColorAsState(
        if (isDefault) onContainer else MaterialTheme.colorScheme.onSurfaceVariant
    )

    SettingsItem {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            SettingsIconBadge(
                icon = icon,
                containerColor = badgeContainer,
                contentColor = badgeContent,
                shape = badgeShape
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        AnimatedContent(
            targetState = isDefault,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) { default ->
            if (default) {
                Surface(
                    shape = CircleShape,
                    color = container,
                    contentColor = onContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ButtonDefaults.MinHeight)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(
                            text = defaultLabel,
                            style = MaterialTheme.typography.labelLargeEmphasized
                        )
                    }
                }
            } else {
                Button(
                    onClick = onClick,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = action,
                        contentColor = onAction
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLargeEmphasized
                    )
                }
            }
        }
    }
}
