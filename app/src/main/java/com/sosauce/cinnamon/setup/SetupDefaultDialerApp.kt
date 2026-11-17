@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.setup

import androidx.annotation.StringRes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.createDefaultDialerIntent

@Composable
fun SetupDefaultDialerScreen(
    onNext: () -> Unit,
    @StringRes description: Int = R.string.default_dialer_app_desc,
    onSkip: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val launcher = rememberRoleRequestLauncher(onGranted = onNext)

    SetupStep(
        icon = R.drawable.phone_filled,
        title = stringResource(R.string.default_dialer_app),
        description = stringResource(description),
        shape = MaterialShapes.Clover8Leaf,
        buttonText = stringResource(R.string.request_permission),
        onClick = { launcher.launchRoleRequest(context.createDefaultDialerIntent()) },
        onSkip = onSkip
    )
}
