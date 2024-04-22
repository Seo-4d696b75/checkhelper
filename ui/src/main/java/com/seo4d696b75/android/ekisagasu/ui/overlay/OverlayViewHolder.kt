package com.seo4d696b75.android.ekisagasu.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import com.seo4d696b75.android.ekisagasu.domain.dataset.PrefectureRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.search.NearStation
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.databinding.OverlayNotificationBinding
import com.seo4d696b75.android.ekisagasu.ui.utils.formatDistance
import com.seo4d696b75.android.ekisagasu.ui.utils.setAnimationListener
import java.util.Timer
import java.util.TimerTask
import kotlin.math.roundToInt

/**
 * @author Seo-4d696b75
 * @version 2021/01/20.
 */
@SuppressLint("ClickableViewAccessibility")
class OverlayViewHolder(
    private val context: Context,
    private val prefectureRepository: PrefectureRepository,
    private val main: Handler,
    wakeupCallback: () -> Unit,
    selectLineCallback: () -> Unit,
    stopNavigationCallback: () -> Unit,
) {
    private var wakeupCallback: (() -> Unit)? = wakeupCallback

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val animAppear = AnimationUtils.loadAnimation(context, R.anim.anim_appear)
    private val animDisappear = AnimationUtils.loadAnimation(context, R.anim.anim_disappear)
    private val animOpen = AnimationUtils.loadAnimation(context, R.anim.anim_open)
    private val animClose = AnimationUtils.loadAnimation(context, R.anim.anim_close)

    private val icon: View
    private val keepOnScreen: View
    private val touchScreen: View
    private val darkScreen: View
    private val notification: OverlayNotificationBinding

    val navigation: NavigationView

    init {
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

        navigation = NavigationView(
            context,
            layerType,
            windowManager,
            icon,
            selectLineCallback,
            stopNavigationCallback,
        )

        // 一番上に重ねて表示
        windowManager.addView(icon, iconLayoutParam)

        icon.setOnClickListener {
            if (isNavigationRunning) {
                navigation.toggleNavigation()
            } else if (keepNotification) {
                toggleNotification()
            } else {
                onNotificationRemoved(null)
            }
        }
    }

    private val timeNow = context.getString(R.string.notification_time_now)
    private val timeSec = context.getString(R.string.notification_time_sec)
    private val timeMin = context.getString(R.string.notification_time_min)

    private var detectedTime: Long = 0L
    private var nearestStation: NearStation? = null
    private var displayedStation: NearStation? = null
    private var requestedStation: NearStation? = null

    var displayPrefecture: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                invalidatePrefecture(displayedStation)
            }
        }

    var notify: Boolean = true
        set(value) {
            field = value
            if (!value) {
                onNotificationRemoved(null)
            }
        }

    var keepNotification: Boolean = false
        set(value) {
            field = value
            checkKeepNotification()
        }

    var forceNotify: Boolean = false

    var isSearchRunning: Boolean = false
        set(value) {
            field = value
            if (!value) {
                onNotificationRemoved(null)
            }
        }

    var isNavigationRunning: Boolean = false
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

    var brightness: Float = 255f
        set(value) {
            if (value != field && value >= MIN_BRIGHTNESS && value < 256f) {
                field = value
                val black = ColorDrawable((255 - value.roundToInt()).shl(24))
                darkScreen.background = black
            }
        }

    var nightMode: Boolean = false
        set(value) {
            if (value == field) return
            field = value
            setNightMode(value, nightModeTimeout)
        }

    var nightModeTimeout: Int = 0
        set(value) {
            if (value == field || value < 0) return
            field = value
            setNightMode(nightMode, value)
        }

    private var timeoutCallback: Runnable? = null

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
                timeoutCallback?.let { main.removeCallbacks(it) }
                val callback =
                    Runnable {
                        timeoutCallback = null
                        darkScreen.visibility = View.VISIBLE
                    }
                timeoutCallback = callback
                main.postDelayed(callback, 1000L * nightModeTimeout)
            }
        } else {
            timeoutCallback?.let {
                main.removeCallbacks(it)
                timeoutCallback = null
            }
        }
    }

    fun onStationChanged(station: NearStation) = synchronized(this) {
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

    private var elapsedTimer: Timer? = null
    private var durationCallback: Runnable? = null

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
        elapsedTimer?.cancel()
        elapsedTimer = Timer().apply {
            val timerTask = object : TimerTask() {
                override fun run() {
                    main.post {
                        val time = (SystemClock.elapsedRealtime() - detectedTime) / 1000L
                        val mes =
                            if (time < 10) {
                                timeNow
                            } else if (time < 60) {
                                timeSec
                            } else {
                                (time / 60L).toString() + timeMin
                            }
                        notification.textNotificationTime.text = mes
                    }
                }
            }
            schedule(timerTask, 0, 1000)
        }
        if (timer) {
            durationCallback?.let { main.removeCallbacks(it) }
            val callback = Runnable {
                durationCallback = null
                onNotificationRemoved(station.station)
            }
            durationCallback = callback
            main.postDelayed(callback, 5000)
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
        elapsedTimer?.cancel()
        elapsedTimer = null
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

    fun release() {
        windowManager.removeView(icon)
        windowManager.removeView(keepOnScreen)
        windowManager.removeViewImmediate(touchScreen)
        windowManager.removeView(darkScreen)
        windowManager.removeView(notification.root)
        nightMode = false

        icon.setOnClickListener(null)
        touchScreen.setOnTouchListener(null)

        elapsedTimer?.cancel()
        elapsedTimer = null
        durationCallback?.let {
            main.removeCallbacks(it)
            durationCallback = null
        }
        timeoutCallback?.let {
            main.removeCallbacks(it)
            timeoutCallback = null
        }

        navigation.release()
        wakeupCallback = null
    }
}
