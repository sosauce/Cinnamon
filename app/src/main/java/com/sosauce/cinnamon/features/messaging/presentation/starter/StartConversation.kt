@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.features.messaging.presentation.starter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import com.skydoves.cloudy.sky
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import com.sosauce.cinnamon.core.ui.components.buttons.CuteNavigationButtonSurface
import com.sosauce.cinnamon.core.ui.components.items.CuteListItem
import com.sosauce.cinnamon.core.ui.components.searchbars.CuteSearchbar
import com.sosauce.cinnamon.features.contacts.presentation.ContactListItem
import com.sosauce.cinnamon.features.contacts.presentation.components.dialogs.NumberPickerDialog
import com.sosauce.cinnamon.core.utils.LazyListKeys
import com.sosauce.cinnamon.core.utils.beautifyNumber
import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.cinnamon.core.utils.getThreadIdOrCreate
import com.sosauce.cinnamon.core.utils.selfAlignHorizontally
import com.sosauce.cinnamon.features.contacts.domain.CuteContact2
import com.sosauce.nekobites.components.LoadingBox
import com.sosauce.nekobites.components.NoXFound

@Composable
fun SharedTransitionScope.StartConversation(
    state: StartConversationState,
    textFieldState: TextFieldState,
    onNavigateUp: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onToggleGroupChatMode: () -> Unit,
    onAddNumberToGroup: (String) -> Unit
) {
    val context = LocalContext.current
    var contactPhoneNumbersPicker by remember { mutableStateOf<List<String>?>(null) }

    if (contactPhoneNumbersPicker != null) {
        NumberPickerDialog(
            onDismissRequest = { contactPhoneNumbersPicker = null },
            onPickNumber = { number ->
                contactPhoneNumbersPicker = null
                if (state.isGroupChatMode) {
                    onAddNumberToGroup(number)
                } else {
                    onNavigate(Screen.ConversationDetails(number.getThreadIdOrCreate(context)))
                }
            },
            phoneNumbers = contactPhoneNumbersPicker!!
        )
    }




    Scaffold(
        bottomBar = {
            CuteSearchbar(
                modifier = Modifier.selfAlignHorizontally(),
                textFieldState = textFieldState,
                onNavigate = onNavigate,
                navigationIcon = {
                    CuteNavigationButtonSurface(
                        onNavigateUp = onNavigateUp
                    )
                },
                fab = {
                    AnimatedVisibility(
                        visible = state.isGroupChatMode,
                        enter = slideInVertically(bouncySpec()) { it } + fadeIn(),
                        exit = slideOutVertically(bouncySpec()) { it } + fadeOut()
                    ) {
                        FilledIconButton(
                            onClick = {
                                val threadId = state.selectedNumbers.getThreadIdOrCreate(context)
                                onNavigate(Screen.ConversationDetails(threadId))
                            },
                            shape = MaterialShapes.Cookie9Sided.toShape(),
                            modifier = Modifier.size(56.dp),
                            enabled = state.selectedNumbers.size > 1
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = null
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LoadingBox(
            isLoading = state.isLoading
        ) {
            LazyColumn(
                contentPadding = paddingValues
            ) {

                item(LazyListKeys.GROUP_CHAT_BUTTON) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        FilledTonalButton(
                            onClick = onToggleGroupChatMode,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                        ) {

                            val icon =
                                if (state.isGroupChatMode) R.drawable.contact else R.drawable.group_add
                            val text =
                                if (state.isGroupChatMode) R.string.new_chat else R.string.new_group_chat

                            Icon(
                                painter = painterResource(icon),
                                contentDescription = null
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = stringResource(text)
                            )
                        }
                    }
                }


                item(LazyListKeys.SEND_TO) {
                    val input = textFieldState.text.toString().beautifyNumber()

                    AnimatedVisibility(
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = 5.dp),
                        visible = input.isNotEmpty() && input.any { !it.isLetter() },
                        enter = slideInHorizontally { -it } + fadeIn(),
                        exit = slideOutHorizontally { -it } + fadeOut()
                    ) {
                        CuteListItem(
                            modifier = Modifier.animateItem(),
                            onClick = {

                                val threadId = input.getThreadIdOrCreate(context)

                                onNavigate(
                                    Screen.ConversationDetails(threadId)
                                )
                            },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = MaterialShapes.Circle.toShape()
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.add),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.send_to_number, input),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = input,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                }

                if (state.contacts.isNotEmpty()) {

                    val (favorites, nonFavorites) = state.contacts.partition { it.isFavorite }

                    if (favorites.isNotEmpty()) {
                        item(LazyListKeys.FAVORITE_CONTACTS) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                                    .animateItem(),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.favorite_filled),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = pluralStringResource(R.plurals.favorites, favorites.size),
                                    style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                        with(this@StartConversation) {
                            items(
                                items = favorites,
                                key = { contact -> contact.id }
                            ) { contact ->

                                val isSelected = remember(state.selectedNumbers) {
                                    contact.details.phoneNumbers.fastAny {
                                        state.selectedNumbers.contains(
                                            it.number
                                        )
                                    }
                                }

                                ContactListItem(
                                    modifier = Modifier.animateItem(),
                                    contact = CuteContact2(),
                                    isSelected = isSelected,
                                    onClick = {
                                        val phoneNumbers = contact.details.phoneNumbers
                                        val firstNumber =
                                            phoneNumbers.firstOrNull()?.number ?: return@ContactListItem

                                        when {
                                            phoneNumbers.size > 1 -> {
                                                contactPhoneNumbersPicker =
                                                    phoneNumbers.fastMap { it.number }
                                            }

                                            state.isGroupChatMode -> onAddNumberToGroup(firstNumber)
                                            else -> {
                                                val threadId = firstNumber.getThreadIdOrCreate(context)
                                                onNavigate(Screen.ConversationDetails(threadId))
                                            }
                                        }
                                    },
                                    showNumber = true
                                )
                            }
                        }
                    }


                    nonFavorites.groupBy { it.displayName.firstOrNull()?.uppercaseChar() ?: '#' }
                        .toSortedMap().forEach { (letter, contacts) ->
                            item {
                                Text(
                                    text = letter.toString(),
                                    style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .animateItem()
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                )
                            }
                            with(this@StartConversation) {
                                items(
                                    items = contacts,
                                    key = { contact -> contact.id }
                                ) { contact ->

                                    val isSelected = remember(state.selectedNumbers) {
                                        contact.details.phoneNumbers.fastAny {
                                            state.selectedNumbers.contains(
                                                it.number
                                            )
                                        }
                                    }
                                    ContactListItem(
                                        modifier = Modifier.animateItem(),
                                        contact = CuteContact2(),
                                        isSelected = isSelected,
                                        onClick = {
                                            val phoneNumbers = contact.details.phoneNumbers
                                            val firstNumber =
                                                phoneNumbers.firstOrNull()?.number
                                                    ?: return@ContactListItem

                                            when {
                                                phoneNumbers.size > 1 -> {
                                                    contactPhoneNumbersPicker =
                                                        phoneNumbers.fastMap { it.number }
                                                }

                                                state.isGroupChatMode -> onAddNumberToGroup(firstNumber)
                                                else -> {
                                                    val threadId =
                                                        firstNumber.getThreadIdOrCreate(context)
                                                    onNavigate(Screen.ConversationDetails(threadId))
                                                }
                                            }
                                        },
                                        showNumber = true
                                    )
                                }
                            }
                        }
                } else {
                    item {
                        NoXFound(
                            headlineText = R.string.no_contacts_found,
                            bodyText = R.string.no_contacts_found_starter_desc,
                            icon = R.drawable.contacts
                        )
                    }
                }
            }
        }
    }
}