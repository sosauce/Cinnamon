@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.setup

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.rememberIsLandscape
import com.sosauce.cinnamon.core.ui.components.CuteHero

@Composable
fun SetupStep(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    shape: RoundedPolygon,
    buttonText: String,
    onClick: () -> Unit,
    onSkip: (() -> Unit)? = null
) {
    val isLandscape = rememberIsLandscape()

    if (isLandscape) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                CuteHero(
                    icon = icon,
                    title = title,
                    description = description,
                    shape = shape,
                    size = 140.dp
                )
            }
            SetupButtons(
                icon = icon,
                text = buttonText,
                onClick = onClick,
                onSkip = onSkip,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                CuteHero(
                    icon = icon,
                    title = title,
                    description = description,
                    shape = shape
                )
            }
            SetupButtons(
                icon = icon,
                text = buttonText,
                onClick = onClick,
                onSkip = onSkip
            )
        }
    }
}

/**
 * @param onSkip shows a skip button under the main one when provided
 */
@Composable
private fun SetupButtons(
    @DrawableRes icon: Int,
    text: String,
    onClick: () -> Unit,
    onSkip: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
    ) {
        Button(
            onClick = onClick,
            shapes = ButtonDefaults.shapes(),
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
            modifier = Modifier
                .fillMaxWidth()
                .height(ButtonDefaults.MediumContainerHeight)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.MediumIconSize)
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMediumEmphasized
            )
        }
        if (onSkip != null) {
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onSkip,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(
                    text = stringResource(R.string.skip_for_now),
                    style = MaterialTheme.typography.labelLargeEmphasized
                )
            }
        }
    }
}

/**
 * Launches a default-app role request made with createDefaultSmsIntent/createDefaultDialerIntent.
 * Does nothing when the role doesn't exist on this device (nothing can handle the intent then).
 */
fun ManagedActivityResultLauncher<Intent, ActivityResult>.launchRoleRequest(intent: Intent) {
    try {
        launch(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

/**
 * Launcher for a default-app role request: calls [onGranted] when the role was granted,
 * otherwise asks the user to grant it.
 */
@Composable
fun rememberRoleRequestLauncher(
    onGranted: () -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    val context = LocalContext.current
    val grantPermissionMessage = stringResource(R.string.grant_me)

    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onGranted()
        } else {
            Toast.makeText(context, grantPermissionMessage, Toast.LENGTH_SHORT).show()
        }
    }
}
