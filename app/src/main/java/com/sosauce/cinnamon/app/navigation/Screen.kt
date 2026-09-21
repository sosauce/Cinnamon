package com.sosauce.cinnamon.app.navigation

import androidx.navigation3.runtime.NavKey
import com.sosauce.cinnamon.features.contacts.domain.CuteContact
import com.sosauce.cinnamon.features.contacts.domain.CuteContactDetails
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen : NavKey {


    @Serializable
    data object Test : Screen()

    @Serializable
    data object Conversations : Screen()

    @Serializable
    data object Contacts : Screen()

    @Serializable
    data object DebugCall : Screen()

    @Serializable
    data object Dialer : Screen()

    @Serializable
    /**
     * @param prefilledNumber for dial intent
     */
    data class Dialpad(val prefilledNumber: String = "") : Screen()

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
    data class AboutConversation(val threadId: Long) : Screen()

    @Serializable
    data class Forwarding(
        val messageToForward: String
    ) : Screen()

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
        val rawContactId: Long?,
        val prefilledNumber: String = ""
    ) : Screen()

    @Serializable
    data class ConversationDetails(
        val threadId: Long,
        val prefilledMessage: String = ""
    ) : Screen()


}