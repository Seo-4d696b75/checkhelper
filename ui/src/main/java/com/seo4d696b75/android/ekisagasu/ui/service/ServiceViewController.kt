package com.seo4d696b75.android.ekisagasu.ui.service

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.AlarmClock
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.seo4d696b75.android.ekisagasu.domain.lifecycle.AppFinishUseCase
import com.seo4d696b75.android.ekisagasu.domain.lifecycle.BootUseCase
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.message.AppMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.user.UserSettingRepository
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.navigator.NavigatorViewController
import com.seo4d696b75.android.ekisagasu.ui.notification.NotificationViewController
import com.seo4d696b75.android.ekisagasu.ui.overlay.OverlayViewController
import com.seo4d696b75.android.ekisagasu.ui.vibrator.VibratorController
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class ServiceViewController @Inject constructor(
    private val locationRepository: LocationRepository,
    private val userSettingRepository: UserSettingRepository,
    private val searchRepository: StationSearchRepository,
    private val appStateRepository: AppStateRepository,
    private val bootUseCase: BootUseCase,
    private val appFinishUseCase: AppFinishUseCase,
    private val vibratorController: VibratorController,
    private val notificationViewController: NotificationViewController,
    private val overlayViewController: OverlayViewController,
    private val navigatorViewController: NavigatorViewController,
) : BroadcastReceiver() {

    private var context: Context? = null

    val appFinish = appStateRepository.message.filterIsInstance<AppMessage.FinishApp>()

    /**
     * 必要ならActivity側にも通知して終了させる
     */
    fun requestAppFinish() {
        appStateRepository.emitMessage(AppMessage.FinishApp)
    }

    fun onCreate(context: Context, owner: LifecycleOwner) {
        notificationViewController.onCreate(context, owner)
        overlayViewController.onCreate(context, owner)
        navigatorViewController.onCreate(context, owner)
        vibratorController.onCreate(context, owner)

        owner.lifecycleScope.launch {
            launch {
                bootUseCase()
            }
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    userSettingRepository
                        .setting
                        .collect {
                            // TODO flowで流す
                            searchRepository.setSearchK(it.searchK)
                            if (locationRepository.isRunning.value) {
                                locationRepository.startWatchCurrentLocation(it.locationUpdateInterval)
                            }
                        }
                }
                launch {
                    appStateRepository
                        .message
                        .filterIsInstance<AppMessage.StartTimer>()
                        .collect {
                            setTimer()
                        }
                }
            }
        }
    }

    suspend fun onDestroy() {
        Timber.d("terminate service")
        locationRepository.stopWatchCurrentLocation()
        appFinishUseCase()

        notificationViewController.onDestroy()
        overlayViewController.onDestroy()
        navigatorViewController.onDestroy()
        vibratorController.onDestroy()

        context = null
    }

    fun getNotification() = notificationViewController.notification

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.action?.let {
            when (it) {
                Intent.ACTION_SCREEN_OFF -> {
                    overlayViewController.screen = false
                }

                Intent.ACTION_USER_PRESENT -> {
                    overlayViewController.screen = true
                }

                else -> {}
            }
        }
    }

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
