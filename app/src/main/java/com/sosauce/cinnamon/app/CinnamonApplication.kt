@file:OptIn(KoinExperimentalAPI::class)

package com.sosauce.cinnamon.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.providers.RecipientPhoneKeyer
import com.sosauce.cinnamon.app.providers.RecipientPhotoFetcher
import com.sosauce.cinnamon.core.telephony.message.MessageNotificationManager
import com.sosauce.cinnamon.core.telephony.phone.CallNotificationManager
import com.sosauce.cinnamon.core.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration

class CinnamonApplication : Application(), KoinStartup, SingletonImageLoader.Factory {

    companion object {
        @Volatile
        var isCallActivityVisible: Boolean = false
        // StateFlow for overlay manager to observe visibility changes
        val isCallActivityVisibleFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: android.app.Activity) {
                if (activity is com.sosauce.cinnamon.features.phone.presentation.call.CallActivity) {
                    isCallActivityVisible = true
                    isCallActivityVisibleFlow.value = true
                }
            }
            override fun onActivityPaused(activity: android.app.Activity) {
                if (activity is com.sosauce.cinnamon.features.phone.presentation.call.CallActivity) {
                    isCallActivityVisible = false
                    isCallActivityVisibleFlow.value = false
                }
            }
            override fun onActivityCreated(a: android.app.Activity, b: android.os.Bundle?) {}
            override fun onActivityStarted(a: android.app.Activity) {}
            override fun onActivityStopped(a: android.app.Activity) {}
            override fun onActivitySaveInstanceState(a: android.app.Activity, b: android.os.Bundle) {}
            override fun onActivityDestroyed(a: android.app.Activity) {
                if (a is com.sosauce.cinnamon.features.phone.presentation.call.CallActivity) {
                    isCallActivityVisible = false
                    isCallActivityVisibleFlow.value = false
                }
            }
        })
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager


        val messagesName = getString(R.string.incoming_messages)
        val callsName = getString(R.string.calls)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val messageChannel = NotificationChannel(
            MessageNotificationManager.INCOMING_MESSAGES_CHANNEL_ID,
            messagesName,
            importance
        ).apply {
            group = MessageNotificationManager.MESSAGES_GROUP
        }
        val callChannel = NotificationChannel(
            CallNotificationManager.CALLS_CHANNEL_ID,
            callsName,
            importance
        ).apply {
            group = CallNotificationManager.CALLS_GROUP
            setSound(
                ringtone,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(
                MessageNotificationManager.MESSAGES_GROUP,
                getString(R.string.messages)
            )
        )
        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(
                CallNotificationManager.CALLS_GROUP,
                getString(R.string.calls)
            )
        )

        notificationManager.createNotificationChannel(messageChannel)
        notificationManager.createNotificationChannel(callChannel)

    }


    override fun onKoinStartup() = koinConfiguration {
        androidContext(this@CinnamonApplication)
        modules(appModule)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(RecipientPhoneKeyer())
                add(RecipientPhotoFetcher.Factory())
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}