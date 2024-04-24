package com.seo4d696b75.android.ekisagasu.ui.service

import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.seo4d696b75.android.ekisagasu.ui.notification.NotificationViewController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import timber.log.Timber
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 *
 * Main service providing core function in background.
 * This service has to sense GPS location, so must be run as foreground service.
 */
@AndroidEntryPoint
class StationService : LifecycleService() {
    inner class StationServiceBinder : Binder() {
        fun bind(): StationService {
            return this@StationService
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Timber.tag("Service").d("onBind: client requests to bind service")
        return StationServiceBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.tag("Service").d("onUnbind: client unbinds service")
        return true
    }

    override fun onRebind(intent: Intent?) {
        Timber.tag("Service").d("onRebind: client binds service again")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)

        Timber.tag("Service").d("service received start-command")

        intent?.let {
            if (it.hasExtra(KEY_REQUEST)) {
                when (it.getStringExtra(KEY_REQUEST)) {
                    REQUEST_EXIT_SERVICE -> {
                        viewController.requestAppFinish()
                    }

                    REQUEST_START_TIMER -> {
                        viewController.setTimer()
                    }

                    else -> {
                        Timber.tag("Service").w(
                            "unknown intent extra received:%s",
                            it.getStringExtra(KEY_REQUEST),
                        )
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        // init view controller
        viewController.onCreate(this, this)

        // start this service as foreground one
        startForeground(
            NotificationViewController.NOTIFICATION_TAG,
            viewController.getNotification(),
        )

        // register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(viewController, filter)

        viewController
            .appFinish
            .flowWithLifecycle(lifecycle)
            .take(1)
            .onEach {
                unregisterReceiver(viewController)
                viewController.onDestroy()
                stopSelf()
            }
            .launchIn(lifecycleScope)
    }

    @Inject
    lateinit var viewController: ServiceViewController

    companion object {
        const val KEY_REQUEST = "service_request"
        const val REQUEST_EXIT_SERVICE = "exit_service"
        const val REQUEST_START_TIMER = "start_timer"
    }
}
