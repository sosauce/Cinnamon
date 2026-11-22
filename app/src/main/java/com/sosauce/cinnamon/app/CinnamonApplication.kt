@file:OptIn(KoinExperimentalAPI::class)

package com.sosauce.cinnamon.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.datastore.UserPreferences
import com.sosauce.cinnamon.core.di.appModule
import com.sosauce.cinnamon.core.telephony.message.MessageNotificationManager
import com.sosauce.cinnamon.features.incall.CallNotificationManager
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration

class CinnamonApplication : Application(), KoinStartup, SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        val userPreferences by inject<UserPreferences>()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(
                "CinnamonCrash",
                "FATAL CRASH on thread ${thread.name}: ${throwable.message}",
                throwable
            )

            runBlocking {
                userPreferences.saveCrashLogs(throwable.stackTraceToString())
            }

            defaultHandler?.uncaughtException(thread, throwable)
        }


        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager


        val messagesName = getString(R.string.incoming_messages)
        val callsName = getString(R.string.calls)
        val importance = NotificationManager.IMPORTANCE_HIGH
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
            setSound(null, null)
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
        notificationManager.deleteNotificationChannel(CallNotificationManager.LEGACY_CALLS_CHANNEL_ID)

    }


    override fun onKoinStartup() = koinConfiguration {
        androidContext(this@CinnamonApplication)
        modules(appModule)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
//                if (SDK_INT >= 28) {
//                    add(AnimatedImageDecoder.Factory())
//                } else {
//                    add(GifDecoder.Factory())
//                }
            }
            .crossfade(true)
            .build()
    }
}