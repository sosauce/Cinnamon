package com.sosauce.cuteconnect.ui.screens.contacts

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sosauce.cuteconnect.domain.model.AboutMe
import com.sosauce.cuteconnect.ui.shared_components.CuteNavigationButton
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText
import com.sosauce.cuteconnect.ui.shared_components.DefaultContactIcon
import com.sosauce.cuteconnect.utils.ICON_TEXT_SPACING

@Composable
fun AboutMeScreen(
    aboutMe: AboutMe = AboutMe(),
    onNavigateBack: () -> Unit,
) {
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DefaultContactIcon(
                    firstLetter = aboutMe.name.first(),
                    modifier = Modifier.padding(start = 10.dp),
                    size = 170.dp
                )
                Spacer(Modifier.height(20.dp))
                CuteText(
                    text = aboutMe.name,
                    fontSize = 20.sp,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(Modifier.height(25.dp))
                Card(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        CuteText("About")
                        Spacer(Modifier.height(20.dp))
                        Row {
                            Icon(
                                imageVector = Icons.Rounded.Call,
                                contentDescription = null
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Spacer(Modifier.width(ICON_TEXT_SPACING.dp))
                                Column {
                                    CuteText(aboutMe.number)
                                    CuteText(
                                        text = "Mobile",
                                        color = Color.Gray,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                    }
                }

            }
            CuteNavigationButton(
                modifier = Modifier.align(Alignment.BottomStart)
            ) { onNavigateBack() }
        }
    }
}