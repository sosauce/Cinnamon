@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.settings

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.createDefaultDialerIntent
import com.sosauce.cinnamon.core.utils.createDefaultSmsIntent
import com.sosauce.cinnamon.settings.components.SettingsWithTitle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import sv.lib.squircleshape.SquircleShape

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
        // Refresh status after returning
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
        SettingsWithTitle(title = R.string.default_apps) {
            // Header
            Text(
                text = stringResource(R.string.default_apps_desc),
                style = MaterialTheme.typography.bodySmallEmphasized.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = SquircleShape(16.dp),
                            color = if (isDefaultDialer) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.phone_filled),
                                contentDescription = null,
                                tint = if (isDefaultDialer) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Default dialer",
                                style = MaterialTheme.typography.titleSmallEmphasized.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isDefaultDialer) "Cinnamon is default" else "Not default - calls may open system dialer",
                                style = MaterialTheme.typography.labelSmallEmphasized.copy(
                                    color = if (isDefaultDialer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            )
                            Text(
                                text = "Required for call screen & bubble over other apps",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
                // Action button — expressive pill
                if (isDefaultDialer) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Is default dialer",
                                style = MaterialTheme.typography.labelLargeEmphasized.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            // Fix: RoleManager intent must be launched without NEW_TASK for ActivityResultLauncher
                            val roleIntent = context.createDefaultDialerIntent()
                            if (roleIntent.action != null && roleIntent.action!!.isNotEmpty()) {
                                try {
                                    dialerLauncher.launch(roleIntent)
                                } catch (_: Exception) {
                                    // Fallback: try with NEW_TASK via startActivity
                                    try { context.startActivity(roleIntent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                                }
                            } else {
                                // Fallback for older devices / when RoleManager not available
                                val fallback = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                                }
                                try {
                                    dialerLauncher.launch(fallback)
                                } catch (_: Exception) {
                                    try { context.startActivity(fallback.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                                }
                            }
                        },
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Set as default dialer",
                            style = MaterialTheme.typography.labelLargeEmphasized.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // SMS Card
            Card(
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = SquircleShape(16.dp),
                            color = if (isDefaultSms) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.message_rounded),
                                contentDescription = null,
                                tint = if (isDefaultSms) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Default messaging",
                                style = MaterialTheme.typography.titleSmallEmphasized.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isDefaultSms) "Cinnamon is default" else "Not default - messages may open system app",
                                style = MaterialTheme.typography.labelSmallEmphasized.copy(
                                    color = if (isDefaultSms) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                            )
                            Text(
                                text = "Required for SMS/MMS handling",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
                if (isDefaultSms) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Is default messaging",
                                style = MaterialTheme.typography.labelLargeEmphasized.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            val intent = context.createDefaultSmsIntent()
                            if (intent.action?.isNotEmpty() == true) {
                                smsLauncher.launch(intent)
                            }
                        },
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Set as default SMS", style = MaterialTheme.typography.labelLargeEmphasized.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Tips: grant overlay permission for bubble over other apps (Settings → Apps → Cinnamon → Display over other apps). Full-screen requires \"Display over other apps\" + notification permission.",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}
