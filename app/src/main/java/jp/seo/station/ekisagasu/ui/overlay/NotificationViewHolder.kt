package jp.seo.station.ekisagasu.ui.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.service.StationService
import jp.seo.station.ekisagasu.ui.MainActivity

/**
 * @author Seo-4d696b75
 * @version 2020/12/24.
 */
class NotificationViewHolder(
    private val ctx: Context
) {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "jp.seo.station.ekisagasu.notification_main"
        const val NOTIFICATION_TAG = 3910
    }

    private val notificationManager =
        ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val builder: NotificationCompat.Builder
    private var remoteView: RemoteViews? = null
    private var updateCnt = 0

    init {
        // init notification channel
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "MainNotification",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = "This is main notification"
        notificationManager.createNotificationChannel(channel)

        builder = NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID)

        // pending intent to MainActivity
        val intent = Intent(ctx, MainActivity::class.java)
        val stackBuilder = TaskStackBuilder.create(ctx)
        stackBuilder.addNextIntentWithParentStack(intent)
        builder.setContentIntent(
            stackBuilder.getPendingIntent(
                3902,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        )

        // set custom view
        builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        builder.setSmallIcon(R.drawable.notification_icon)
        createNotificationView()
    }

    val notification: Notification
        get() = builder.build()

    val needNotificationSetting: Boolean by lazy {
        val channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
        val vibrate = channel.shouldVibrate()
        val sound = channel.sound
        if (vibrate || sound != null) {
            // user must edit notification channel setting
            channel.enableVibration(false)
            channel.enableLights(false)
            channel.setSound(null, null)
            return@lazy true
        }
        false
    }

    private fun createNotificationView() {
        val view = RemoteViews(ctx.packageName, R.layout.notification_main)
        val exit = Intent(ctx, StationService::class.java)
            .putExtra(StationService.KEY_REQUEST, StationService.REQUEST_EXIT_SERVICE)
        view.setOnClickPendingIntent(
            R.id.notificationButton1,
            PendingIntent.getService(
                ctx,
                1,
                exit,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
            )
        )
        val timer = Intent(ctx, StationService::class.java)
            .putExtra(StationService.KEY_REQUEST, StationService.REQUEST_START_TIMER)
        view.setOnClickPendingIntent(
            R.id.notificationButton2,
            PendingIntent.getService(
                ctx,
                2,
                timer,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        )
        builder.setCustomContentView(view)
        remoteView = view
    }

    fun update(title: String, message: String) {
        synchronized(this) {
            if (updateCnt++ > 100) {
                // refresh view
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
