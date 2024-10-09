package com.seo4d696b75.android.ekisagasu.ui.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.permission.PermissionRepository.Companion.NOTIFICATION_CHANNEL_ID
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.ui.MainActivity
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.service.StationService
import com.seo4d696b75.android.ekisagasu.ui.utils.formatDistance
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2020/12/24.
 */
class NotificationViewController @Inject constructor(
    private val searchRepository: StationSearchRepository,
    private val locationRepository: LocationRepository,
) {
    companion object {
        const val NOTIFICATION_TAG = 3910
    }

    private lateinit var notificationManager: NotificationManager
    private lateinit var builder: NotificationCompat.Builder

    private var remoteView: RemoteViews? = null
    private var updateCnt = 0
    private var _context: Context? = null

    fun onCreate(context: Context, owner: LifecycleOwner) {
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        _context = context

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

        createNotificationView(context)

        owner.lifecycleScope.launch {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // 最近傍の駅の変化
                    searchRepository
                        .result
                        .filterNotNull()
                        .map { it.nearest }
                        .collect { s ->
                            update(
                                String.format("%s  %s", s.station.name, s.getDetectedTime()),
                                String.format("%s   %s", s.distance.formatDistance, s.getLinesName()),
                            )
                        }
                }
                launch {
                    // 探索状態の変化
                    locationRepository.isRunning.collect {
                        if (it) {
                            update(
                                context.getString(R.string.notification_title_start),
                                context.getString(R.string.notification_message_start),
                            )
                        } else {
                            update(
                                context.getString(R.string.notification_title_wait),
                                context.getString(R.string.notification_message_wait),
                            )
                        }
                    }
                }
                launch {
                    // TODO notificationとは関係なくね？
                    // 探索終了
                    locationRepository
                        .isRunning
                        .filter { !it }
                        .drop(1)
                        .collect {
                            Toast.makeText(
                                context,
                                context.getString(R.string.message_stop_search),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                }
            }
        }
    }

    val notification: Notification
        get() = builder.build()

    private fun createNotificationView(context: Context) {
        val view = RemoteViews(context.packageName, R.layout.notification_main)
        builder.setCustomContentView(view)
        remoteView = view
    }

    private fun update(
        title: String,
        message: String,
    ) {
        val context = _context ?: return
        synchronized(this) {
            if (updateCnt++ > 100) {
                // RemoteViewを一定回数以上更新すると不具合が発生する場合があるので作り直す
                updateCnt = 0
                createNotificationView(context)
            }
            remoteView?.let {
                it.setTextViewText(R.id.notification_title, title)
                it.setTextViewText(R.id.notification_message, message)
            }
            notificationManager.notify(NOTIFICATION_TAG, notification)
        }
    }

    fun onDestroy() {
        _context = null
    }
}
