package com.seo4d696b75.android.ekisagasu.ui.service

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.AlarmClock
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistryOwner
import com.seo4d696b75.android.ekisagasu.domain.lifecycle.AppFinishUseCase
import com.seo4d696b75.android.ekisagasu.domain.lifecycle.BootUseCase
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.message.AppMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.navigator.NavigatorViewController
import com.seo4d696b75.android.ekisagasu.ui.notification.NotificationViewController
import com.seo4d696b75.android.ekisagasu.ui.overlay.OverlayViewController
import com.seo4d696b75.android.ekisagasu.ui.popup.PopupViewController
import com.seo4d696b75.android.ekisagasu.ui.vibrator.VibratorController
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceViewController @Inject constructor(
    private val locationRepository: LocationRepository,
    private val appStateRepository: AppStateRepository,
    private val bootUseCase: BootUseCase,
    private val appFinishUseCase: AppFinishUseCase,
    private val vibratorController: VibratorController,
    private val notificationViewController: NotificationViewController,
    private val overlayViewController: OverlayViewController,
    private val popupViewController: PopupViewController,
    private val navigatorViewController: NavigatorViewController,
) {

    private var context: Context? = null

    val appFinish = appStateRepository.appFinish.take(1)

    /**
     * 必要ならActivity側にも通知して終了させる
     */
    suspend fun requestAppFinish() {
        appStateRepository.requestAppFinish()
    }

    fun onCreate(
        context: Context,
        registryOwner: SavedStateRegistryOwner,
        lifecycleOwner: LifecycleOwner,
    ) {
        this.context = context

        notificationViewController.onCreate(context, lifecycleOwner)
        overlayViewController.onCreate(context, lifecycleOwner)
        popupViewController.onCreate(context, registryOwner, lifecycleOwner)
        navigatorViewController.onCreate(context, registryOwner, lifecycleOwner)
        vibratorController.onCreate(context, lifecycleOwner)

        lifecycleOwner.lifecycleScope.launch {
            launch {
                bootUseCase()
            }

            launch {
                appStateRepository
                    .message
                    .flowWithLifecycle(lifecycleOwner.lifecycle)
                    .filterIsInstance<AppMessage.StartTimer>()
                    .collect {
                        setTimer()
                    }
            }

            launch {
                appFinish.collect {
                    onDestroy()
                }
            }
        }
    }

    private suspend fun onDestroy() {
        Timber.d("terminate service")
        locationRepository.stopWatchCurrentLocation()
        appFinishUseCase()

        notificationViewController.onDestroy()
        overlayViewController.onDestroy()
        popupViewController.onDestroy()
        navigatorViewController.onDestroy()
        vibratorController.onDestroy()

        context = null
    }

    fun getNotification() = notificationViewController.notification

    private val timerDurationMillis = 5 * 60 * 1000L
    private var previousTimerTimestamp = -timerDurationMillis

    fun setTimer() {
        val context = this.context ?: return
        val current = SystemClock.elapsedRealtime()
        if (current - previousTimerTimestamp < timerDurationMillis) {
            Toast.makeText(context, context.getString(R.string.timer_wait_message), Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_MESSAGE, context.getString(R.string.timer_title))
            .putExtra(AlarmClock.EXTRA_LENGTH, 300)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            Toast.makeText(context, context.getString(R.string.timer_set_message), Toast.LENGTH_SHORT).show()
            previousTimerTimestamp = current
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "Failed to set timer")
        }
    }
}
