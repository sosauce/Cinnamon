package com.sosauce.cinnamon.presentation.navigation

import androidx.navigation3.runtime.NavKey
import com.sosauce.cinnamon.domain.model.CuteContact
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen: NavKey {


    @Serializable
    data object Test : Screen()
    @Serializable
    data object Messages : Screen()

    @Serializable
    data object Contacts : Screen()

    @Serializable
    data object DebugCall : Screen()

    @Serializable
    data object Dialer : Screen()

    @Serializable
    data object Dialpad : Screen()

    @Serializable
    data object Voicemail : Screen()


    @Serializable
    data object StartConversation : Screen()

    @Serializable
    data object AboutMe : Screen()

    @Serializable
    data object Settings : Screen()

    @Serializable
    data object ArchivedThreads : Screen()

    @Serializable
    data class ConversationTheming(
        val threadId: Long
    ) : Screen()

    @Serializable
    data object DebugMms : Screen()

    @Serializable
    data class ContactDetails(
        val contactId: Long
    ) : Screen()

    @Serializable
    data class ContactEditor(
        val contact: CuteContact
    ) : Screen()

    @Serializable
    data class Conversation(
        val threadId: Long
    ) : Screen()


}