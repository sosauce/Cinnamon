package com.sosauce.cinnamon.setup

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.HOW_TO_ENABLE_RESTRCITED_PERMS

@Composable
fun SetupScreen(
    onGotoApp: () -> Unit
) {
    var setupProgress by remember { mutableIntStateOf(0) }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        bottomBar = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                Card(
                    modifier = Modifier
                        .padding(
                            horizontal = 20.dp,
                            vertical = 10.dp
                        )
                        .navigationBarsPadding(),
                    onClick = { uriHandler.openUri(HOW_TO_ENABLE_RESTRCITED_PERMS) },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = contentColorFor(MaterialTheme.colorScheme.error)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.warning),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.android_16_warning),
                            style = MaterialTheme.typography.bodyMediumEmphasized
                        )
                    }
                }
            }
        }
    ) { pv ->
        Crossfade(
            modifier = Modifier
                .padding(pv)
                .padding(horizontal = 10.dp),
            targetState = setupProgress
        ) { progress ->
            when (progress) {
                0 -> SetupDefaultMessageApp { setupProgress = 1 }
                1 -> SetupDefaultDialerScreen(onGotoApp)
            }
        }

    }

}