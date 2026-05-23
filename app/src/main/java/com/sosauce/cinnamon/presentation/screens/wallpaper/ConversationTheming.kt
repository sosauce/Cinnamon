@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.presentation.screens.wallpaper

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.data.conversation_settings.ConversationSettingActions
import com.sosauce.cinnamon.presentation.shared_components.CategoryCard
import com.sosauce.cinnamon.presentation.shared_components.ImagePickerCard
import com.sosauce.cinnamon.presentation.shared_components.buttons.CuteNavigationButton
import com.sosauce.cinnamon.presentation.shared_components.buttons.WavySlider
import com.sosauce.cinnamon.presentation.shared_components.text.HeaderText
import com.sosauce.cinnamon.utils.selfAlignHorizontally
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@Composable
fun ConversationTheming(
    state: ThemingState,
    threadId: Long,
    onHandleConversationSettingsActions: (ConversationSettingActions) -> Unit,
    onNavigateBack: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showColorPicker by remember { mutableStateOf(false) }
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->

            if (uri == null) return@rememberLauncherForActivityResult

            scope.launch(Dispatchers.IO) {

                File(state.settings.wallpaper).delete()

                val file = File(
                    context.filesDir,
                    "wallpaper_${threadId}_${System.currentTimeMillis()}.jpg"
                )

                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }

                onHandleConversationSettingsActions(
                    ConversationSettingActions.UpsertConversationSettings(
                        state.settings.copy(
                            wallpaper = file.path
                        )
                    )
                )
            }
        }

    if (showColorPicker) {
        ColorPickerDialog(
            onDismissRequest = { showColorPicker = false },
            onAddNewColor = { color ->
                onHandleConversationSettingsActions(
                    ConversationSettingActions.UpsertConversationSettings(
                        state.settings.copy(
                            color = color
                        )
                    )
                )
            }
        )
    }

    Scaffold(
        bottomBar = {
            CuteNavigationButton(
                modifier = Modifier
                    .selfAlignHorizontally(Alignment.Start)
                    .navigationBarsPadding(),
                onNavigateUp = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            HeaderText("Wallpaper")

            ImagePickerCard(
                onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemoveImage = {
                    scope.launch(Dispatchers.IO) {
                        File(context.filesDir, state.settings.wallpaper).delete()
                        onHandleConversationSettingsActions(
                            ConversationSettingActions.UpsertConversationSettings(
                                state.settings.copy(
                                    wallpaper = ""
                                )
                            )
                        )
                    }
                },
                imagePath = state.settings.wallpaper,
                blur = state.settings.wallpaperBlurIntensity,
                modifier = Modifier
                    .selfAlignHorizontally()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .height(280.dp)
                    .width(200.dp)
            )
            var tempSliderValue by remember { mutableStateOf<Float?>(null) }
            val value = tempSliderValue ?: state.settings.wallpaperBlurIntensity.toFloat()
            HeaderText(stringResource(R.string.blur_intensity) + " (${value.roundToInt()}%)")
            CategoryCard(
                topDp = 24.dp,
                bottomDp = 24.dp
            ) {


                WavySlider(
                    value = value,
                    onValueChange = { tempSliderValue = it },
                    onValueChangeFinished = {
                        tempSliderValue?.let {
                            onHandleConversationSettingsActions(
                                ConversationSettingActions.UpsertConversationSettings(
                                    state.settings.copy(
                                        wallpaperBlurIntensity = it.roundToInt(),
                                    )
                                )
                            )
                        }
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.padding(10.dp)
                )
            }

            HeaderText("Chat color")
            val color =
                if (state.settings.color != -1) Color(state.settings.color) else MaterialTheme.colorScheme.surfaceContainer
            Card(
                onClick = { showColorPicker = true },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = color,
                    contentColor = if (color.luminance() > 0.5f) Color.Black else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
            ) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val icon =
                        if (state.settings.color != -1) R.drawable.edit_filled else R.drawable.colorize

                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }

}

