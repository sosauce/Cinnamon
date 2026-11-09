@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.setup

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.HOW_TO_ENABLE_RESTRCITED_PERMS

private const val StepCount = 2

@Composable
fun SetupScreen(
    onGotoApp: () -> Unit
) {
    var setupProgress by rememberSaveable { mutableIntStateOf(0) }
    val uriHandler = LocalUriHandler.current
    val progress by animateFloatAsState(
        targetValue = (setupProgress + 1) / StepCount.toFloat(),
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec()
    )

    Scaffold(
        bottomBar = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                Surface(
                    onClick = { uriHandler.openUri(HOW_TO_ENABLE_RESTRCITED_PERMS) },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.warning),
                            contentDescription = null
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.android_16_warning),
                            style = MaterialTheme.typography.bodyMediumEmphasized
                        )
                    }
                }
            }
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
        ) {
            LinearWavyProgressIndicator(
                progress = { progress },
                stopSize = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 1.dp)
            )
            AnimatedContent(
                targetState = setupProgress,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it / 4 } + fadeOut()
                },
                modifier = Modifier.fillMaxSize()
            ) { step ->
                when (step) {
                    0 -> SetupDefaultMessageApp { setupProgress = 1 }
                    1 -> SetupDefaultDialerScreen(onGotoApp)
                }
            }
        }
    }
}
