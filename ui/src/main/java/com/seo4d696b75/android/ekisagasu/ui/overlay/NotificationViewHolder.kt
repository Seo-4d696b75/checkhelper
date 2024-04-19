package com.seo4d696b75.android.ekisagasu.ui.overlay

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.seo4d696b75.android.ekisagasu.domain.permission.PermissionRepository.Companion.NOTIFICATION_CHANNEL_ID
import com.seo4d696b75.android.ekisagasu.ui.MainActivity
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.service.StationService

/**
 * @author Seo-4d696b75
 * @version 2020/12/24.
 */
class NotificationViewHolder(private val context: Context) {
    companion object {
        const val NOTIFICATION_TAG = 3910
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
    private var remoteView: RemoteViews? = null
    private var updateCnt = 0

    init {
        // pending intent to MainActivity
        val intent = Intent(context, MainActivity::class.java)
        val stackBuilder = TaskStackBuilder.create(context)
        stackBuilder.addNextIntentWithParentStack(intent)
        builder.setContentIntent(
            stackBuilder.getPendingIntent(
                3902,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )

        // set custom view
        builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        builder.setSmallIcon(R.drawable.notification_icon)

        // action button
        val exit = Intent(context, StationService::class.java)
            .putExtra(StationService.KEY_REQUEST, StationService.REQUEST_EXIT_SERVICE)
        val timer = Intent(context, StationService::class.java)
            .putExtra(StationService.KEY_REQUEST, StationService.REQUEST_START_TIMER)
        builder.addAction(
            R.drawable.notification_exit,
            context.getString(R.string.notification_action_exit),
            PendingIntent.getService(
                context,
                1,
                exit,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        builder.addAction(
            R.drawable.notification_timer,
            context.getString(R.string.notification_action_timer),
            PendingIntent.getService(
                context,
                2,
                timer,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )

        createNotificationView()
    }

    val notification: Notification
        get() = builder.build()

    private fun createNotificationView() {
        val view = RemoteViews(context.packageName, R.layout.notification_main)
        builder.setCustomContentView(view)
        remoteView = view
    }

    fun update(
        title: String,
        message: String,
    ) {
        synchronized(this) {
            if (updateCnt++ > 100) {
                // RemoteViewを一定回数以上更新すると不具合が発生する場合があるので作り直す
                updateCnt = 0
                createNotificationView()
            }
            remoteView?.let {
                it.setTextViewText(R.id.notification_title, title)
                it.setTextViewText(R.id.notification_message, message)
            }
            notificationManager.notify(NOTIFICATION_TAG, notification)
        }
    }
}
