@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.core.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.utils.appVersion
import com.sosauce.nekobites.components.Spacer
import androidx.core.net.toUri

@Composable
fun CrashScreen(
    crashLog: String,
    onClose: () -> Unit
) {

    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(50)
                    )
                    .padding(10.dp)
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = {

                        val text = "Device information: SDK v${Build.VERSION.SDK_INT} \n\n $crashLog"
                        val mailtoUri = "mailto:".toUri()
                            .buildUpon()
                            .appendQueryParameter("to", "sosauce_dev@protonmail.com")
                            .appendQueryParameter("subject", "[Crash] Cinnamon v${context.appVersion}")
                            .appendQueryParameter("body", text)
                            .build()

                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = mailtoUri
                        }

                        context.startActivity(Intent.createChooser(emailIntent,"Choose an Email client : "))


                    },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = contentColorFor(MaterialTheme.colorScheme.secondaryContainer)
                    ),
                    modifier = Modifier.weight(1f).size(IconButtonDefaults.mediumContainerSize(
                        IconButtonDefaults.IconButtonWidthOption.Wide))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.email),
                        contentDescription = null
                    )
                    Spacer(5.dp)
                    Text(stringResource(R.string.email))
                }
                Button(
                    onClick = onClose,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = contentColorFor(MaterialTheme.colorScheme.primaryContainer)
                    ),
                    modifier = Modifier.weight(1f).size(IconButtonDefaults.mediumContainerSize(
                        IconButtonDefaults.IconButtonWidthOption.Wide))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null
                    )
                    Spacer(5.dp)
                    Text(stringResource(R.string.close))
                }
            }
        }
    ) { pv ->

        LazyColumn(
            contentPadding = pv + PaddingValues(horizontal = 5.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(55.dp)
                            .background(
                                color = MaterialTheme.colorScheme.error,
                                shape = MaterialShapes.Cookie9Sided.toShape()
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.warning),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onError
                        )
                    }
                    Spacer(5.dp)
                    Column {
                        Text(
                            text = "Cinnamon crashed last time you used it!",
                            style = MaterialTheme.typography.titleMediumEmphasized
                        )
                        Text(
                            text = "You can report the crash if you want",
                            style = MaterialTheme.typography.labelMediumEmphasized.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                            RoundedCornerShape(24.dp)
                        )
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = crashLog,
                        modifier = Modifier.padding(15.dp)
                    )
                }
            }
        }

    }


}


@Preview
@Composable
private fun CrashScreenPreview() {

    CinnamonTheme {


        CrashScreen("java.lang.RuntimeException: Can't create handler inside thread Thread[DefaultDispatcher-worker-1,5,main] that has not called Looper.prepare()\n\tat android.os.Handler.<init>(Handler.java:261)\n\tat android.os.Handler.<init>(Handler.java:165)\n\tat android.view.ViewRootImpl\$ViewRootHandler.<init>(ViewRootImpl.java:7373)\n\tat android.view.ViewRootImpl.<init>(ViewRootImpl.java:7789)\n\tat android.view.ViewRootImpl.<init>(ViewRootImpl.java:1402)\n\tat android.view.WindowManagerGlobal.addView(WindowManagerGlobal.java:504)\n\tat android.view.WindowManagerImpl.addView(WindowManagerImpl.java:189)\n\tat com.sosauce.cinnamon.core.system.services.CallOverlayManager.showOverlay(CallOverlayManager.kt:107)\n\tat com.sosauce.cinnamon.core.system.services.CallOverlayManager\$observe\$4.invokeSuspend(CallOverlayManager.kt:58)\n\tat com.sosauce.cinnamon.core.system.services.CallOverlayManager\$observe\$4.invoke(CallOverlayManager.kt:8)\n\tat com.sosauce.cinnamon.core.system.services.CallOverlayManager\$observe\$4.invoke(CallOverlayManager.kt:4)\n\tat kotlinx.coroutines.flow.FlowKt__TransformKt\$onEach\$\$inlined\$unsafeTransform\$1\$2.emit(Emitters.kt:217)\n\tat kotlinx.coroutines.flow.FlowKt__ZipKt\$combine\$1\$1.invokeSuspend(Zip.kt:29)\n\tat kotlinx.coroutines.flow.FlowKt__ZipKt\$combine\$1\$1.invoke(Zip.kt:13)\n\tat kotlinx.coroutines.flow.FlowKt__ZipKt\$combine\$1\$1.invoke(Zip.kt:6)\n\tat kotlinx.coroutines.flow.internal.CombineKt\$combineInternal\$2.invokeSuspend(Combine.kt:73)\n\tat kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:34)\n\tat kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:100)\n\tat kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:586)\n\tat kotlinx.coroutines.scheduling.CoroutineScheduler\$Worker.executeTask(CoroutineScheduler.kt:807)\n\tat kotlinx.coroutines.scheduling.CoroutineScheduler\$Worker.runWorker(CoroutineScheduler.kt:717)\n\tat kotlinx.coroutines.scheduling.CoroutineScheduler\$Worker.run(CoroutineScheduler.kt:704)", {})
    }
}