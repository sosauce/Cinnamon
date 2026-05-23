@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.setup

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.presentation.screens.setup.components.SetupBottomBar
import com.sosauce.cinnamon.utils.HOW_TO_ENABLE_RESTRCITED_PERMS
import com.sosauce.cinnamon.utils.requestRole

@SuppressLint("InlinedApi")
@Composable
fun SetupPermissions(
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
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        bottomBar = {
            SetupBottomBar(
                hasPermissions = isSmsRoleHeld && isDialerRoleHeld,
                isLastStep = true,
                onGoToNextPage = {},
                onNavigateToApp = notifyHasRoles
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(10.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {

                val uriHandler = LocalUriHandler.current

                Text(
                    text = "On Android 16 and above, you'll have to allow restricted permissions in order to use the app.",
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { uriHandler.openUri(HOW_TO_ENABLE_RESTRCITED_PERMS) },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text("Learn how")
                }
                Spacer(Modifier.height(10.dp))
            }

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
                        painter = if (isSmsRoleHeld) painterResource(R.drawable.check) else painterResource(
                            R.drawable.close
                        ),
                        contentDescription = null,
                        tint = if (isSmsRoleHeld) Color.Green.copy(0.85f) else Color.Red.copy(0.85f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(stringResource(R.string.set_as_sms_app))
                        Text(
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
                        painter = if (isDialerRoleHeld) painterResource(R.drawable.check) else painterResource(
                            R.drawable.close
                        ),
                        contentDescription = null,
                        tint = if (isDialerRoleHeld) Color.Green.copy(0.85f) else Color.Red.copy(
                            0.85f
                        )
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(stringResource(R.string.set_as_dialer_app))
                        Text(
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
