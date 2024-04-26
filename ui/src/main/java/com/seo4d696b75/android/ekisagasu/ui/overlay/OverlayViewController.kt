package com.seo4d696b75.android.ekisagasu.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.seo4d696b75.android.ekisagasu.domain.dataset.PrefectureRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.search.NearStation
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.user.UserSettingRepository
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.databinding.OverlayNotificationBinding
import com.seo4d696b75.android.ekisagasu.ui.utils.formatDistance
import com.seo4d696b75.android.ekisagasu.ui.utils.setAnimationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * @author Seo-4d696b75
 * @version 2021/01/20.
 */
@SuppressLint("ClickableViewAccessibility")
class OverlayViewController @Inject constructor(
    private val locationRepository: LocationRepository,
    private val searchRepository: StationSearchRepository,
    private val settingRepository: UserSettingRepository,
    private val appStateRepository: AppStateRepository,
    private val navigatorRepository: NavigatorRepository,
    private val prefectureRepository: PrefectureRepository,
) {
    // callback
    private var wakeupCallback: (() -> Unit)? = null

    // coroutine
    private var _lifecycleScope: CoroutineScope? = null
    private val scope: CoroutineScope
        get() = requireNotNull(_lifecycleScope)

    // view & window manager
    private lateinit var windowManager: WindowManager

    private lateinit var icon: View
    private lateinit var keepOnScreen: View
    private lateinit var touchScreen: View
    private lateinit var darkScreen: View
    private lateinit var notification: OverlayNotificationBinding

    // UI表示のリソース
    private lateinit var animAppear: Animation
    private lateinit var animDisappear: Animation
    private lateinit var animOpen: Animation
    private lateinit var animClose: Animation

    private lateinit var timeNow: String
    private lateinit var timeSec: String
    private lateinit var timeMin: String

    // notification 表示制御用の変数
    private var detectedTime: Long = 0L
    private var nearestStation: NearStation? = null
    private var displayedStation: NearStation? = null
    private var requestedStation: NearStation? = null

    fun onCreate(context: Context, owner: LifecycleOwner) {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wakeupCallback = {
            val intent = Intent(context, WakeupActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }

        _lifecycleScope = owner.lifecycleScope

        val inflater = LayoutInflater.from(context)
        val layerType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        // transparent & not touchable view so that screen kept turn on
        val keepOnLayoutParam = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            0, 0, layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        )
        keepOnScreen = View(context)
        keepOnScreen.visibility = View.GONE
        windowManager.addView(keepOnScreen, keepOnLayoutParam)

        val darkLayoutParam = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            0, 0, layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        )
        darkScreen = View(context)
        darkScreen.visibility = View.GONE
        windowManager.addView(darkScreen, darkLayoutParam)

        // zero size view to watch any touch event and not consume any touch event
        val touchLayoutParam = WindowManager.LayoutParams(
            0, 0,
            0, 0, layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        )
        touchScreen = View(context).apply {
            setOnTouchListener { _, _ ->
                if (keepOnScreen.visibility == View.VISIBLE) {
                    keepOnScreen.visibility = View.GONE
                    if (!keepNotification) {
                        onNotificationRemoved(null)
                    }
                }
                if (nightModeTimeout > 0 || !nightMode) {
                    darkScreen.visibility = View.GONE
                }
                if (nightMode) {
                    setNightModeTimeout(true)
                }
                false
            }
            isClickable = false
            isLongClickable = false
        }
        windowManager.addView(touchScreen, touchLayoutParam)

        // Push通知
        val notificationLayoutParam = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            0, 0, layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP.or(Gravity.START)
            screenBrightness = -1f
        }
        notification = OverlayNotificationBinding.inflate(inflater)
        notification.root.apply {
            visibility = View.GONE
            setOnClickListener {
                if (keepNotification) {
                    toggleNotification()
                } else {
                    onNotificationRemoved(null)
                }
            }
        }
        windowManager.addView(notification.root, notificationLayoutParam)

        // addViewする順序に注意
        val iconLayoutParam = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            0,
            0,
            layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP.or(Gravity.START)
            screenBrightness = -1.0f
        }
        icon = inflater.inflate(R.layout.overlay_icon, null, false)
        icon.visibility = View.GONE
        windowManager.addView(icon, iconLayoutParam)

        icon.setOnClickListener {
            if (keepNotification) {
                toggleNotification()
            } else {
                onNotificationRemoved(null)
            }
        }

        animAppear = AnimationUtils.loadAnimation(context, R.anim.anim_appear)
        animDisappear = AnimationUtils.loadAnimation(context, R.anim.anim_disappear)
        animOpen = AnimationUtils.loadAnimation(context, R.anim.anim_open)
        animClose = AnimationUtils.loadAnimation(context, R.anim.anim_close)

        timeNow = context.getString(R.string.notification_time_now)
        timeSec = context.getString(R.string.notification_time_sec)
        timeMin = context.getString(R.string.notification_time_min)

        owner.lifecycleScope.launch {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    // 現在の最近傍駅の変化（距離の変化は無視する）
                    searchRepository
                        .result
                        .filterNotNull()
                        .map { it.detected }
                        .distinctUntilChanged()
                        .collect(::onStationChanged)
                }
                launch {
                    // 現在の最近傍駅＆距離
                    searchRepository
                        .result
                        .filterNotNull()
                        .map { it.nearest }
                        .collect(::onLocationChanged)
                }
                launch {
                    // 探索の進行状態を更新
                    locationRepository.isRunning.collect {
                        isSearchRunning = it
                    }
                }
                launch {
                    // 設定の変更
                    settingRepository.setting.collect {
                        notify = it.isPushNotification
                        keepNotification = it.isKeepNotification
                        forceNotify = it.isPushNotificationForce
                        displayPrefecture = it.isShowPrefectureNotification
                        nightModeTimeout = it.nightModeTimeout
                        brightness = it.nightModeBrightness
                    }
                }
                launch {
                    // nightモード切替
                    appStateRepository.nightMode.collect {
                        nightMode = it
                    }
                }
                launch {
                    // 経路探索のon/off
                    navigatorRepository.isRunning.collect {
                        isNavigationRunning = it
                    }
                }
            }
        }

    }

    private var displayPrefecture: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                invalidatePrefecture(displayedStation)
            }
        }

    private var notify: Boolean = true
        set(value) {
            field = value
            if (!value) {
                onNotificationRemoved(null)
            }
        }

    private var keepNotification: Boolean = false
        set(value) {
            field = value
            checkKeepNotification()
        }

    private var forceNotify: Boolean = false

    private var isSearchRunning: Boolean = false
        set(value) {
            field = value
            if (!value) {
                onNotificationRemoved(null)
            }
        }

    private var isNavigationRunning: Boolean = false
        set(value) {
            field = value
            checkKeepNotification()
        }

    private fun checkKeepNotification() {
        if (isSearchRunning && keepNotification && !isNavigationRunning) {
            nearestStation?.let { onNotifyStation(it, false) }
        } else {
            onNotificationRemoved(null)
        }
    }

    var screen: Boolean = true
        set(value) =
            synchronized(this) {
                if (value != field) {
                    field = value
                    if (value) {
                        requestedStation?.let { request ->
                            requestedStation = null
                            onNotifyStation(request, !keepNotification)
                        }
                    }
                    if (!value && nightMode && nightModeTimeout > 0) {
                        darkScreen.visibility = View.GONE
                    }
                    if (nightMode) {
                        setNightModeTimeout(value)
                    }
                }
            }

    companion object {
        const val MIN_BRIGHTNESS = 20f
    }

    private var brightness: Float = 255f
        set(value) {
            if (value != field && value >= MIN_BRIGHTNESS && value < 256f) {
                field = value
                val black = ColorDrawable((255 - value.roundToInt()).shl(24))
                darkScreen.background = black
            }
        }

    private var nightMode: Boolean = false
        set(value) {
            if (value == field) return
            field = value
            setNightMode(value, nightModeTimeout)
        }

    private var nightModeTimeout: Int = 0
        set(value) {
            if (value == field || value < 0) return
            field = value
            setNightMode(nightMode, value)
        }

    private var nightModeTimeoutJob: Job? = null

    private fun setNightMode(
        enable: Boolean,
        timeout: Int,
    ) {
        if (enable) {
            if (timeout == 0) {
                darkScreen.visibility = View.VISIBLE
            } else {
                setNightModeTimeout(true)
            }
        } else {
            setNightModeTimeout(false)
            darkScreen.visibility = View.GONE
            keepOnScreen.visibility = View.GONE
        }
    }

    private fun setNightModeTimeout(set: Boolean) {
        if (set) {
            if (nightModeTimeout > 0 && nightMode) {
                nightModeTimeoutJob?.cancel()
                nightModeTimeoutJob = scope.launch {
                    delay(1000L * nightModeTimeout)
                    darkScreen.visibility = View.VISIBLE
                    nightModeTimeoutJob = null
                }
            }
        } else {
            nightModeTimeoutJob?.cancel()
            nightModeTimeoutJob = null
        }
    }

    private fun onStationChanged(station: NearStation) = synchronized(this) {
        detectedTime = SystemClock.elapsedRealtime()
        nearestStation = station
        if (!notify) return@synchronized
        if (isNavigationRunning) return@synchronized
        if (screen) {
            if (nightMode && nightModeTimeout > 0 && darkScreen.visibility == View.VISIBLE) {
                keepOnScreen.visibility = View.VISIBLE
                onNotifyStation(station, false)
            } else {
                onNotifyStation(station, !keepNotification)
            }
        } else if (forceNotify) {
            wakeupCallback?.invoke()
            keepOnScreen.visibility = View.VISIBLE
            darkScreen.visibility = View.VISIBLE
            onNotifyStation(station, false)
        } else {
            requestedStation = station
        }
    }

    fun onLocationChanged(station: NearStation) {
        if (requestedStation?.station == station.station) {
            requestedStation = station
        }
        notification.textNotificationDistance.text = station.distance.formatDistance
    }

    private var updateNotificationJob: Job? = null

    private fun onNotifyStation(
        station: NearStation,
        timer: Boolean,
    ) {
        displayedStation = station
        invalidatePrefecture(station)
        notification.stationNameNotification.text = station.station.name
        notification.textNotificationLines.text = station.getLinesName()
        onLocationChanged(station)

        // start animation
        notification.root.visibility = View.VISIBLE
        notification.contentContainer.startAnimation(animAppear)

        updateNotificationJob?.cancel()
        updateNotificationJob = scope.launch {
            // 1秒ごとに表示更新
            launch {
                while (isActive) {
                    val time = (SystemClock.elapsedRealtime() - detectedTime) / 1000L
                    val mes = if (time < 10) {
                        timeNow
                    } else if (time < 60) {
                        timeSec
                    } else {
                        (time / 60L).toString() + timeMin
                    }
                    notification.textNotificationTime.text = mes
                    delay(1000L)
                }
            }
            // 通知を消す（必要なら）
            if (timer) {
                launch {
                    delay(5000L)
                    onNotificationRemoved(station.station)
                }
            }
        }
    }

    private fun onNotificationRemoved(station: Station?) {
        val current = displayedStation ?: return
        if (station != null && current.station != station) return
        if (notification.root.visibility != View.VISIBLE && icon.visibility != View.VISIBLE) return
        if (notification.root.visibility == View.VISIBLE) {
            animDisappear.setAnimationListener(onEnd = {
                notification.root.visibility = View.GONE
            })
            notification.contentContainer.startAnimation(animDisappear)
        } else {
            icon.visibility = View.GONE
        }
        displayedStation = null
        updateNotificationJob?.cancel()
        updateNotificationJob = null
    }

    private fun invalidatePrefecture(station: NearStation?) {
        val show = displayPrefecture
        if (show != (notification.textNotificationPrefecture.visibility == View.VISIBLE)) {
            notification.textNotificationPrefecture.visibility = if (show) View.VISIBLE else View.INVISIBLE
        }
        if (show && station != null) {
            notification.textNotificationPrefecture.text = prefectureRepository.getName(station.station.prefecture)
        }
    }

    private fun toggleNotification() {
        if (notification.root.visibility == View.VISIBLE) {
            animClose.setAnimationListener(onEnd = {
                notification.root.visibility = View.GONE
            })
            notification.contentContainer.startAnimation(animClose)
            icon.visibility = View.VISIBLE
        } else {
            notification.root.visibility = View.VISIBLE
            animOpen.setAnimationListener(onEnd = {
                icon.visibility = View.GONE
            })
            notification.contentContainer.startAnimation(animOpen)
        }
    }

    fun onDestroy() {
        windowManager.removeView(icon)
        windowManager.removeView(keepOnScreen)
        windowManager.removeViewImmediate(touchScreen)
        windowManager.removeView(darkScreen)
        windowManager.removeView(notification.root)
        nightMode = false

        icon.setOnClickListener(null)
        touchScreen.setOnTouchListener(null)

        updateNotificationJob?.cancel()
        updateNotificationJob = null

        nightModeTimeoutJob?.cancel()
        nightModeTimeoutJob = null

        wakeupCallback = null

        _lifecycleScope = null
    }
}
