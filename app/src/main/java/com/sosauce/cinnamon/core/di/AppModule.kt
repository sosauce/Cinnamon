package com.sosauce.cinnamon.core.di

import android.content.Context
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import androidx.room.Room
import androidx.work.WorkManager
import com.sosauce.cinnamon.core.MediaManager
import com.sosauce.cinnamon.core.NumberLookup
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.core.repository.SimsRepository
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsDao
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.ContactSettingsDatabase
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingsDao
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.ConversationSettingsDatabase
import com.sosauce.cinnamon.features.messaging.data.local.scheduledMessages.ScheduledMessagesDao
import com.sosauce.cinnamon.features.messaging.data.local.scheduledMessages.ScheduledMessagesDatabase
import com.sosauce.cinnamon.features.contacts.data.repository.ContactsRepository
import com.sosauce.cinnamon.features.messaging.data.repository.MessagesRepository
import com.sosauce.cinnamon.features.phone.data.repository.CallLogsRepository
import com.sosauce.cinnamon.features.messaging.data.repository.ConversationsRepository
import com.sosauce.cinnamon.features.phone.data.repository.VoicemailsRepository
import com.sosauce.cinnamon.core.telephony.PhoneNumberNormalizer
import com.sosauce.cinnamon.core.telephony.message.CuteTelephonyManager
import com.sosauce.cinnamon.core.telephony.message.MessageNotificationManager
import com.sosauce.cinnamon.core.system.services.CallOverlayManager
import com.sosauce.cinnamon.core.telephony.phone.CallManager
import com.sosauce.cinnamon.core.telephony.phone.CallNotificationManager
import com.sosauce.cinnamon.features.contacts.data.local.contactSettings.MIGRATION_1_2_CONTACT_SETTINGS
import com.sosauce.cinnamon.features.messaging.presentation.archived.ArchivedConversationsViewModel
import com.sosauce.cinnamon.features.contacts.presentation.ContactDetailsViewModel
import com.sosauce.cinnamon.features.contacts.presentation.ContactsViewModel
import com.sosauce.cinnamon.features.contacts.presentation.editor.EditContactViewModel
import com.sosauce.cinnamon.features.messaging.data.ScheduledMessageManager
import com.sosauce.cinnamon.features.messaging.data.local.conversationSettings.MIGRATION_1_2_CONVERSATION_SETTINGS
import com.sosauce.cinnamon.features.messaging.data.local.scheduledMessages.MIGRATION_1_2_SCHEDULED_MESSAGE
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationDetailsViewModel
import com.sosauce.cinnamon.features.messaging.presentation.conversation.ConversationsViewModel
import com.sosauce.cinnamon.features.messaging.presentation.conversation.components.bottombar.BottomBarViewModel
import com.sosauce.cinnamon.features.phone.presentation.logs.CallLogsViewModel
import com.sosauce.cinnamon.features.phone.presentation.dialpad.DialpadViewModel
import com.sosauce.cinnamon.features.phone.presentation.call.CallingViewModel
import com.sosauce.cinnamon.settings.MigrationViewModel
import com.sosauce.cinnamon.features.messaging.presentation.starter.StartConversationViewModel
import com.sosauce.cinnamon.features.phone.presentation.voicemail.VoicemailViewModel
import com.sosauce.cinnamon.features.messaging.presentation.customization.ThemingViewModel
import com.sosauce.cinnamon.settings.SimsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {

    single<ConversationSettingsDao> {
        Room.databaseBuilder(
            context = androidContext(),
            klass = ConversationSettingsDatabase::class.java,
            name = "conversationSettingsEntity.db"
        ).addMigrations(MIGRATION_1_2_CONVERSATION_SETTINGS).build().dao
    }

    single<ScheduledMessagesDao> {
        Room.databaseBuilder(
            context = androidContext(),
            klass = ScheduledMessagesDatabase::class.java,
            name = "scheduledMessages.db"
        ).addMigrations(MIGRATION_1_2_SCHEDULED_MESSAGE).build().dao
    }

    single<ContactSettingsDao> {
        Room.databaseBuilder(
            context = androidContext(),
            klass = ContactSettingsDatabase::class.java,
            name = "contactSettingsEntity.db"
        ).addMigrations(MIGRATION_1_2_CONTACT_SETTINGS).build().dao
    }

    single { CoroutineScope(SupervisorJob()) }
    single { WorkManager.getInstance(androidContext()) }

    single<TelecomManager> {
        androidContext().getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    }
    single<TelephonyManager> {
        androidContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    single {
        PhoneNumberNormalizer(
            telephonyManager = get()
        )
    }

    singleOf(::UserPreferences)
    singleOf(::NumberLookup)
    singleOf(::ScheduledMessageManager)
    singleOf(::CallManager)
    singleOf(::MessageNotificationManager)
    singleOf(::CallNotificationManager)
    singleOf(::CallOverlayManager)
    singleOf(::CuteTelephonyManager)
    singleOf(::MessagesRepository)
    singleOf(::ContactsRepository)
    singleOf(::ConversationsRepository)
    singleOf(::CallLogsRepository)
    singleOf(::VoicemailsRepository)
    singleOf(::SimsRepository)
    singleOf(::MediaManager)


    viewModelOf(::ContactsViewModel)
    viewModelOf(::ContactDetailsViewModel)
    viewModelOf(::ConversationDetailsViewModel)
    viewModelOf(::ThemingViewModel)
    viewModelOf(::ConversationsViewModel)
    viewModelOf(::ArchivedConversationsViewModel)
    viewModelOf(::CallLogsViewModel)
    viewModelOf(::VoicemailViewModel)
    viewModelOf(::DialpadViewModel)
    viewModelOf(::CallingViewModel)
    viewModelOf(::BottomBarViewModel)
    viewModelOf(::MigrationViewModel)
    viewModelOf(::StartConversationViewModel)
    viewModelOf(::EditContactViewModel)
    viewModelOf(::SimsViewModel)
}