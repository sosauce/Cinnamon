@file:OptIn(KoinExperimentalAPI::class)

package com.sosauce.cinnamon.main

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
import com.sosauce.cinnamon.data.fetchers.RecipientPhoneKeyer
import com.sosauce.cinnamon.data.fetchers.RecipientPhotoFetcher
import com.sosauce.cinnamon.data.managers.CallNotificationManager
import com.sosauce.cinnamon.data.managers.MessageNotificationManager
import com.sosauce.cinnamon.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration

class App : Application(), KoinStartup, SingletonImageLoader.Factory {


    override fun onCreate() {
        super.onCreate()
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
        androidContext(this@App)
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