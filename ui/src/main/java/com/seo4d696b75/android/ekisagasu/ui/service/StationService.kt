package com.seo4d696b75.android.ekisagasu.ui.service

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.seo4d696b75.android.ekisagasu.ui.broadcast.ScreenBroadcastReceiver
import com.seo4d696b75.android.ekisagasu.ui.notification.NotificationViewController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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
class StationService : Service(), LifecycleOwner, SavedStateRegistryOwner {

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
                        lifecycleScope.launch {
                            viewController.requestAppFinish()
                        }
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

    override fun onBind(intent: Intent?): IBinder? {
        dispatcher.onServicePreSuperOnBind()
        return null
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onStart(intent: Intent?, startId: Int) {
        dispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    private val dispatcher = ServiceLifecycleDispatcher(this)

    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()

        // init view controller
        savedStateRegistryController.performRestore(null)
        viewController.onCreate(this, this, this)

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
        registerReceiver(screenBroadcastReceiver, filter)

        lifecycleScope.launch {
            viewController
                .appFinish
                .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
                .collect {
                    unregisterReceiver(screenBroadcastReceiver)
                    viewModelStore.clear()
                    viewController.onDestroy()

                    // LifecycleService だと stopSelf, onDestroy の間にデータ更新をFlowから購読すると不要な通知が出る場合がある
                    // stopSelf の段階で Lifecycle.State.DESTROYED に更新する
                    dispatcher.onServicePreSuperOnDestroy()

                    stopSelf()
                }
        }
    }

    @Inject
    lateinit var viewController: ServiceViewController

    @Inject
    lateinit var screenBroadcastReceiver: ScreenBroadcastReceiver

    @Inject
    @ServiceViewModel
    lateinit var viewModelStore: ViewModelStore

    companion object {
        const val KEY_REQUEST = "service_request"
        const val REQUEST_EXIT_SERVICE = "exit_service"
        const val REQUEST_START_TIMER = "start_timer"
    }
}
