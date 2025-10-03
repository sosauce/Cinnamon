@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cuteconnect.ui.screens.setup

import android.annotation.SuppressLint
import android.app.role.RoleManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sosauce.cuteconnect.R
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText
import com.sosauce.cuteconnect.utils.requestRole

@SuppressLint("InlinedApi")
@Composable
fun SetupScreen(
    notifyHasRoles: () -> Unit
) {

    val activity = LocalActivity.current!!
    val context = LocalContext.current
    val roleManager = remember(context) { context.getSystemService(RoleManager::class.java) }
    var isSmsRoleHeld by remember { mutableStateOf(roleManager.isRoleHeld(RoleManager.ROLE_SMS)) }
    var isDialerRoleHeld by remember { mutableStateOf(roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isSmsRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_SMS)
                isDialerRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_DIALER)

                if (isDialerRoleHeld && isSmsRoleHeld) { notifyHasRoles() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(10.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
//            Icon(
//                painter = painterResource(R.drawable.app_icon),
//                contentDescription = null,
//                modifier = Modifier.size(64.dp)
//            )
            CuteText(
                text = "Welcome to CuteConnect !",
                style = MaterialTheme.typography.displayLargeEmphasized.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp,
                     lineHeight = 1.1.em
                )
            )
            Spacer(Modifier.height(20.dp))

            LinearWavyProgressIndicator(
                progress = {
                    val progress = mapOf(
                        isDialerRoleHeld to 0.5f,
                        isSmsRoleHeld to 0.5f
                    )

                    progress.filter { it.key }
                        .values
                        .sum()

                },
                stopSize = 0.dp,
                waveSpeed = 15.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(30.dp))

            Card(
                onClick = { activity.requestRole(RoleManager.ROLE_SMS) },
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 4.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSmsRoleHeld) Icons.Rounded.Check else Icons.Rounded.Close,
                        contentDescription = null,
                        tint = if (isSmsRoleHeld) Color.Green.copy(0.85f) else Color.Red.copy(0.85f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        CuteText(stringResource(R.string.set_as_sms_app))
                        CuteText(
                            text = stringResource(
                                if (isSmsRoleHeld) R.string.is_default_sms else R.string.not_default_sms
                            ),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.85f)
                        )
                    }
                }
            }
            Card(
                onClick = { activity.requestRole(RoleManager.ROLE_DIALER) },
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 4.dp,
                    bottomStart = 24.dp,
                    bottomEnd = 24.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDialerRoleHeld) Icons.Rounded.Check else Icons.Rounded.Close,
                        contentDescription = null,
                        tint = if (isDialerRoleHeld) Color.Green.copy(0.85f) else Color.Red.copy(0.85f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        CuteText(stringResource(R.string.set_as_dialer_app))
                        CuteText(
                            text = stringResource(
                                if (isDialerRoleHeld) R.string.is_default_dialer else R.string.not_default_dialer
                            ),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.85f)
                        )
                    }
                }
            }
        }
    }
}
