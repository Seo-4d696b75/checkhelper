package com.seo4d696b75.android.ekisagasu.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.seo4d696b75.android.ekisagasu.data.R
import com.seo4d696b75.android.ekisagasu.domain.lifecycle.AppInitializer
import com.seo4d696b75.android.ekisagasu.domain.permission.PermissionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

class NotificationChannelInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppInitializer {
    override fun onCreate() {
        val notificationManager = requireNotNull(
            context.getSystemService<NotificationManager>()
        )

        // TODO 削除予定
        // delete old channel if any
        notificationManager.deleteNotificationChannel("jp.seo.station.ekisagasu.notification_main")

        val channel = NotificationChannel(
            PermissionRepository.NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(false)
            enableLights(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }
}

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface NotificationChannelInitializerModule {
    @Binds
    @IntoSet
    fun bind(impl: NotificationChannelInitializer): AppInitializer
}
