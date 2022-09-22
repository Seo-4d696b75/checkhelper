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
    private val context: Context
) {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "jp.seo.station.ekisagasu.notification_main_silent"
        const val NOTIFICATION_TAG = 3910
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val builder: NotificationCompat.Builder
    private var remoteView: RemoteViews? = null
    private var updateCnt = 0

    init {
        // TODO 削除予定
        // delete old channel if any
        notificationManager.deleteNotificationChannel("jp.seo.station.ekisagasu.notification_main")

        // init notification channel
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(false)
            enableLights(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)

        builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)

        // pending intent to MainActivity
        val intent = Intent(context, MainActivity::class.java)
        val stackBuilder = TaskStackBuilder.create(context)
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

    fun update(title: String, message: String) {
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
