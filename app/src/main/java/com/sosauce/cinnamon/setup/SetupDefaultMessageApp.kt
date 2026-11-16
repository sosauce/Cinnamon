@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.setup

import android.annotation.SuppressLint
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.createDefaultSmsIntent

@SuppressLint("InlinedApi")
@Composable
fun SetupDefaultMessageApp(
    onNext: () -> Unit
) {

    val context = LocalContext.current
    val launcher = rememberRoleRequestLauncher(onGranted = onNext)

    SetupStep(
        icon = R.drawable.messages_filled,
        title = stringResource(R.string.default_sms_app),
        description = stringResource(R.string.default_sms_app_desc),
        shape = MaterialShapes.Cookie12Sided,
        buttonText = stringResource(R.string.request_permission),
        onClick = { launcher.launch(context.createDefaultSmsIntent()) }
    )
}
