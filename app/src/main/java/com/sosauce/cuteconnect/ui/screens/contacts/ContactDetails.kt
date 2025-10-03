@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cuteconnect.ui.screens.contacts

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import com.sosauce.cuteconnect.R
import com.sosauce.cuteconnect.data.actions.CallAction
import com.sosauce.cuteconnect.data.contact_settings.ContactSettingsActions
import com.sosauce.cuteconnect.data.conversation_settings.ConversationSettingActions
import com.sosauce.cuteconnect.domain.model.CuteContact
import com.sosauce.cuteconnect.ui.navigation.Screen
import com.sosauce.cuteconnect.ui.screens.contacts.components.ContactActionsRow
import com.sosauce.cuteconnect.ui.screens.contacts.components.ContactInfos
import com.sosauce.cuteconnect.ui.shared_components.BottomActionButtons
import com.sosauce.cuteconnect.ui.shared_components.CuteNavigationButton
import com.sosauce.cuteconnect.ui.shared_components.text.CuteText
import com.sosauce.cuteconnect.ui.shared_components.DefaultContactIcon
import com.sosauce.cuteconnect.utils.addOrNot
import com.sosauce.cuteconnect.utils.copyMutate
import com.sosauce.cuteconnect.viewModels.ContactViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ContactDetails(
    contact: CuteContact,
    onNavigateBack: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onHandleCallAction: (CallAction) -> Unit
) {

    val context = LocalContext.current
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val contactViewModel = koinViewModel<ContactViewModel>(
        parameters = { parametersOf(contact.id) }
    )
    val contactSettings by contactViewModel.contactSettings.collectAsStateWithLifecycle()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let { uri ->
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            //
            contactViewModel.handleContactSettingsActions(
                ContactSettingsActions.UpsertContactSettings(
                    contactSettings.copy(
                        poster = uri.toString(),
                    )
                )
            )
        }
    }


    Scaffold { pv ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(pv)
            ) {
                IconButton(
                    onClick = { imagePicker.launch(arrayOf("image/*")) }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = contactSettings.poster.toUri(),
                        contentDescription = null,
                        modifier = Modifier
                            .height(200.dp)
                            .cloudy(30),
                        contentScale = ContentScale.FillWidth
                    )
                    DefaultContactIcon(
                        firstLetter = contact.name.firstOrNull(),
                        modifier = Modifier
                            .padding(start = 10.dp),
                        size = 170.dp,
                        contactPfp = contact.photo,
                        shape = MaterialShapes.Cookie12Sided.toShape()
                    )

                }
                Spacer(Modifier.height(15.dp))
                CuteText(
                    text = contact.name,
                    modifier = Modifier.basicMarquee(),
                    style = MaterialTheme.typography.headlineLargeEmphasized
                )
                Spacer(Modifier.height(15.dp))
                ContactActionsRow(
                    contact = contact,
                    onNavigate = onNavigate,
                    onHandleCallAction = onHandleCallAction
                )
                Spacer(Modifier.height(25.dp))
                ContactInfos(
                    contact = contact,
                    isEditMode = isEditMode
                )

            }

            AnimatedVisibility(
                visible = scrollState.canScrollForward,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)

            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CuteNavigationButton(
                        modifier = Modifier.navigationBarsPadding()
                    ) { onNavigateBack() }
                    BottomActionButtons(
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        Row {
                            IconButton(
                                onClick = { isEditMode = !isEditMode }
                            ) {
                                AnimatedContent(
                                    targetState = isEditMode
                                ) { editMode ->
                                    if (editMode) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = null
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(R.drawable.edit_filled),
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = {}
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.delete_filled),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

            }

        }
    }

}