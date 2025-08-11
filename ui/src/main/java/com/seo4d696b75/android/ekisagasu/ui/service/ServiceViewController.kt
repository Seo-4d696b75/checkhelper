package com.seo4d696b75.android.ekisagasu.ui.service

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistryOwner
import com.seo4d696b75.android.ekisagasu.domain.lifecycle.AppFinishUseCase
import com.seo4d696b75.android.ekisagasu.domain.lifecycle.BootUseCase
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.timer.SetTimerUseCase
import com.seo4d696b75.android.ekisagasu.ui.navigator.NavigatorViewController
import com.seo4d696b75.android.ekisagasu.ui.notification.NotificationViewController
import com.seo4d696b75.android.ekisagasu.ui.overlay.OverlayViewController
import com.seo4d696b75.android.ekisagasu.ui.popup.PopupViewController
import com.seo4d696b75.android.ekisagasu.ui.vibrator.VibratorController
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    private val setTimer: SetTimerUseCase,
) {
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
        notificationViewController.onCreate(context, lifecycleOwner)
        overlayViewController.onCreate(context, lifecycleOwner)
        popupViewController.onCreate(context, registryOwner, lifecycleOwner)
        navigatorViewController.onCreate(context, registryOwner, lifecycleOwner)
        vibratorController.onCreate(context, lifecycleOwner)

        lifecycleOwner.lifecycleScope.launch {
            bootUseCase()
        }
    }

    suspend fun onDestroy() {
        locationRepository.stopWatchCurrentLocation()
        appFinishUseCase()
        notificationViewController.onDestroy()
        overlayViewController.onDestroy()
        popupViewController.onDestroy()
        navigatorViewController.onDestroy()
        vibratorController.onDestroy()
    }

    fun getNotification() = notificationViewController.notification

    fun setTimer() {
        setTimer.invoke()
    }
}
