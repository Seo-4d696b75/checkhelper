package jp.seo.station.ekisagasu.ui.overlay

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.PowerManager
import android.os.SystemClock
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.animation.addListener
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.model.NearStation
import jp.seo.station.ekisagasu.model.Station
import jp.seo.station.ekisagasu.repository.PrefectureRepository
import jp.seo.station.ekisagasu.search.formatDistance
import jp.seo.station.ekisagasu.utils.setAnimationListener
import java.util.*
import kotlin.math.roundToInt

/**
 * @author Seo-4d696b75
 * @version 2021/01/20.
 */
@SuppressLint("ClickableViewAccessibility")
class OverlayViewHolder(
    private val context: Context,
    private val prefectureRepository: PrefectureRepository,
    private val main: Handler
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val keyguardManager =
        context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    private val animAppear = AnimationUtils.loadAnimation(context, R.anim.anim_appear)
    private val animDisappear = AnimationUtils.loadAnimation(context, R.anim.anim_disappear)
    private val animOpen = AnimationUtils.loadAnimation(context, R.anim.anim_open)
    private val animClose = AnimationUtils.loadAnimation(context, R.anim.anim_close)

    private val icon: View
    private val keepOnScreen: View
    private val touchScreen: View
    private val darkScreen: View
    private val notification: View

    private val notificationContainer: View
    private val notificationContent: View

    private val name: TextView
    private val prefecture: TextView
    private val distance: TextView
    private val lines: TextView
    private val time: TextView

    val navigation: NavigationView

    init {
        val inflater = LayoutInflater.from(context)
        val layerType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        var layoutParam = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            0, 0, layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        layoutParam.gravity = Gravity.TOP.or(Gravity.START)
        layoutParam.screenBrightness = -1.0f
        icon = inflater.inflate(R.layout.overlay_icon, null, false)
        icon.visibility = View.GONE
        windowManager.addView(icon, layoutParam)

        // transparent & not touchable view so that screen kept turn on
        layoutParam = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            0, 0, layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        keepOnScreen = View(context)
        keepOnScreen.visibility = View.GONE
        windowManager.addView(keepOnScreen, layoutParam)

        layoutParam = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            0, 0, layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        darkScreen = View(context)
        darkScreen.visibility = View.GONE
        windowManager.addView(darkScreen, layoutParam)

        // zero size view to watch any touch event and not consume any touch event
        layoutParam = WindowManager.LayoutParams(
            0, 0,
            0, 0, layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        touchScreen = View(context)
        touchScreen.setOnTouchListener { v, event ->
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
        touchScreen.isClickable = false
        touchScreen.isLongClickable = false
        windowManager.addView(touchScreen, layoutParam)

        layoutParam = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            0, 0, layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        layoutParam.gravity = Gravity.TOP.or(Gravity.START)
        layoutParam.screenBrightness = -1f
        notification = inflater.inflate(R.layout.overlay_notification, null, false)
        notification.visibility = View.GONE
        notification.setOnClickListener {

            if (keepNotification) {
                toggleNotification()
            } else {
                onNotificationRemoved(null)
            }
        }
        windowManager.addView(notification, layoutParam)
        name = notification.findViewById(R.id.station_name_notification)
        prefecture = notification.findViewById(R.id.text_notification_prefecture)
        distance = notification.findViewById(R.id.text_notification_distance)
        lines = notification.findViewById(R.id.text_notification_lines)
        time = notification.findViewById(R.id.text_notification_time)
        notificationContainer = notification.findViewById(R.id.container_notification)
        notificationContent = notification.findViewById(R.id.container_notification_detail)

        navigation = NavigationView(context, layerType, windowManager, icon)

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
        set(value) = synchronized(this) {
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

    private fun setNightMode(enable: Boolean, timeout: Int) {
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
                val callback = Runnable {
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
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "station-found:"
            )
            wakeLock.acquire(10)
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
        distance.text = formatDistance(station.distance)
    }

    private var elapsedTimer: Timer? = null
    private var durationCallback: Runnable? = null

    private fun onNotifyStation(station: NearStation, timer: Boolean) {
        displayedStation = station
        invalidatePrefecture(station)
        name.text = station.station.name
        lines.text = station.getLinesName()
        onLocationChanged(station)

        // start animation
        notificationContent.visibility = View.VISIBLE
        notification.visibility = View.VISIBLE
        notificationContent.visibility = View.VISIBLE
        notificationContainer.startAnimation(animAppear)
        elapsedTimer?.cancel()
        elapsedTimer = Timer().apply {
            schedule(
                object : TimerTask() {
                    override fun run() {
                        main.post {
                            val time = (SystemClock.elapsedRealtime() - detectedTime) / 1000L
                            val mes = if (time < 10) {
                                timeNow
                            } else if (time < 60) {
                                timeSec
                            } else {
                                (time / 60L).toString() + timeMin
                            }
                            this@OverlayViewHolder.time.text = mes
                        }
                    }
                },
                0, 1000
            )
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
        if (notification.visibility != View.VISIBLE && icon.visibility != View.VISIBLE) return
        if (notification.visibility == View.VISIBLE) {
            animDisappear.setAnimationListener(onEnd = {
                notification.visibility = View.GONE
            })
            notificationContainer.startAnimation(animDisappear)
        } else {
            icon.visibility = View.GONE
        }
        displayedStation = null
        elapsedTimer?.cancel()
        elapsedTimer = null
    }

    private fun invalidatePrefecture(station: NearStation?) {
        val show = displayPrefecture
        if (show != (prefecture.visibility == View.VISIBLE)) {
            prefecture.visibility = if (show) View.VISIBLE else View.INVISIBLE
        }
        if (show && station != null) {
            prefecture.text = prefectureRepository.getName(station.station.prefecture)
        }
    }

    private fun toggleNotification() {
        if (notification.visibility == View.VISIBLE) {
            animClose.setAnimationListener(onEnd = {
                notification.visibility = View.GONE
            })
            notificationContent.startAnimation(animClose)
            icon.visibility = View.VISIBLE
        } else {
            notification.visibility = View.VISIBLE
            notificationContent.visibility = View.VISIBLE
            animOpen.setAnimationListener(onStart = {
                icon.visibility = View.GONE
            })
            notificationContent.startAnimation(animOpen)
        }
    }

    private var timerLayoutParam: WindowManager.LayoutParams? = null
    private var timerView: View? = null
    private var timerContainer: View? = null
    private var timerButton: View? = null

    var timerListener: (() -> Unit)? = null

    private var isTimerButtonClicked = false

    var timerPosition = -1
    private var touchY = -1f

    fun fixTimer(fix: Boolean) {
        setFixedTimer(fix, false)
    }

    fun setTimerState(running: Boolean) {
        toggleTimerIcon(!running, null)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setFixedTimer(enable: Boolean, immediate: Boolean) {
        if (enable) {
            if (timerView == null) {
                val layerType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                val view = View.inflate(context, R.layout.overlay_timer, null)
                timerView = view
                var pos = timerPosition
                if (pos < 0) {
                    pos = Point().apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            context.display?.getRealSize(this)
                        } else {
                            windowManager.defaultDisplay.getRealSize(this)
                        }
                    }.y / 2
                    timerPosition = pos
                }
                val param = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    0, pos, layerType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
                )
                param.gravity = Gravity.END or Gravity.TOP
                timerContainer = view.findViewById(R.id.container_timer)
                timerButton = view.findViewById(R.id.fab_timer_fixed)
                timerButton?.setOnTouchListener { v, event ->
                    timerButton?.let { button ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                isTimerButtonClicked = false
                                touchY = event.rawY
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val y = event.rawY
                                if (isTimerButtonClicked) {
                                    timerLayoutParam?.let { param ->
                                        param.y = (timerPosition + (y - touchY)).toInt()
                                        timerView?.let {
                                            windowManager.updateViewLayout(
                                                it, param
                                            )
                                        }
                                    }
                                } else {
                                    touchY = y
                                }
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_HOVER_EXIT -> {
                                timerLayoutParam?.let { timerPosition = it.y }
                            }
                            else -> {
                            }
                        }
                    }
                    v.onTouchEvent(event)
                }
                timerButton?.setOnClickListener {
                    if (!isTimerButtonClicked) {
                        timerListener?.let { it() }
                    }
                }
                timerButton?.setOnLongClickListener {
                    timerButton?.let {
                        isTimerButtonClicked = true
                        timerLayoutParam?.let { timerPosition = it.y }
                    }
                    false
                }
                windowManager.addView(view, param)
                timerLayoutParam = param
                toggleTimerIcon(true, null)
            }
        } else {
            timerView?.let { view ->
                if (immediate || view.visibility != View.VISIBLE) {
                    windowManager.removeView(view)
                    timerButton?.setOnClickListener(null)
                    timerButton?.setOnLongClickListener(null)
                    timerButton?.setOnTouchListener(null)
                    timerButton = null
                    timerView = null
                    timerContainer = null
                } else {
                    toggleTimerIcon(false) {
                        setFixedTimer(false, true)
                    }
                }
            }
        }
    }

    private fun toggleTimerIcon(show: Boolean, callback: (() -> Unit)?) {
        timerContainer?.let { container ->
            if (show == (container.visibility == View.VISIBLE)) return@let
            val width = container.width
            if (show) container.visibility = View.VISIBLE
            ObjectAnimator.ofInt(
                container,
                "translationX",
                if (show) width else 0,
                if (show) 0 else width
            ).apply {
                duration = 300
                addListener(onEnd = {
                    if (!show) container.visibility = View.GONE
                    it.removeAllListeners()
                    callback?.let { it() }
                })
                start()
            }
        }
    }

    fun release() {
        windowManager.removeView(icon)
        windowManager.removeView(keepOnScreen)
        windowManager.removeViewImmediate(touchScreen)
        windowManager.removeView(darkScreen)
        windowManager.removeView(notification)
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
        timerView?.let {
            windowManager.removeView(it)
            timerView = null
        }
        timerListener = null

        navigation.release()
    }
}
