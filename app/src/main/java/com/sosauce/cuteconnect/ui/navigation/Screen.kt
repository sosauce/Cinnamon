package com.sosauce.cuteconnect.ui.navigation

import androidx.navigation3.runtime.NavKey
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
    data class ConversationTheming(
        val threadId: Long
    ) : Screen()

    @Serializable
    data object DebugMms : Screen()

    @Serializable
    data class ContactDetails(
        val id: Long
    ) : Screen()

    /**
     * @param number Must be plain number with no formatting.
     */
    @Serializable
    data class Conversation(
        val number: String
    ) : Screen()

}