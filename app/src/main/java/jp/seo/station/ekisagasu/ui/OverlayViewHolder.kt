package jp.seo.station.ekisagasu.ui

import android.app.KeyguardManager
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.PowerManager
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.core.NearStation
import jp.seo.station.ekisagasu.core.PrefectureRepository
import jp.seo.station.ekisagasu.search.formatDistance
import java.util.*

/**
 * @author Seo-4d696b75
 * @version 2021/01/20.
 */
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
    private val backScreen: View
    private var nightScreen: View? = null
    private val notification: View

    private val notificationContainer: View
    private val notificationContent: View

    private val name: TextView
    private val prefecture: TextView
    private val distance: TextView
    private val lines: TextView
    private val time: TextView

    init {
        val inflater = LayoutInflater.from(context)
        val layerType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

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
        icon.setOnClickListener {
            if (keepNotification) {
                toggleNotification()
            } else {
                onNotificationRemoved(null)
            }
        }
        windowManager.addView(icon, layoutParam)

        layoutParam = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            0, 0, layerType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        )
        backScreen = View(context)
        backScreen.visibility = View.GONE
        backScreen.setOnClickListener {
            backScreen.visibility = View.GONE
            onNotificationRemoved(null)
        }
        windowManager.addView(backScreen, layoutParam)


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

    var keepNotification: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                if (value) {
                    nearestStation?.let { onNotifyStation(it, false) }
                } else {
                    onNotificationRemoved(null)
                }
            }
        }

    var forceNotify: Boolean = false


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
            }
        }

    var brightness: Int = 255
        set(value) {
            if (value != field && value > 50 && value < 256) {
                field = value
                val black = ColorDrawable((255 - value).shl(24))
                val night = nightScreen
                if (night == null) {
                    backScreen.background = black
                } else {
                    backScreen.background = ColorDrawable(0)
                    night.background = black
                }
            }
        }

    var nightMode: Boolean = false
        set(value) {
            if (value == field) return
            field = value
            if (value) {

                //initialize view

                //initialize view
                val layerType =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
                val night = View(context)
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    0, 0, layerType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                )
                night.background = ColorDrawable((255 - brightness).shl(24))
                backScreen.background = ColorDrawable(0x00000000)
                windowManager.addView(night, params)
                night.visibility = View.VISIBLE
                nightScreen = night
            } else {
                nightScreen?.let {
                    windowManager.removeViewImmediate(it)
                    nightScreen = null
                    backScreen.background = ColorDrawable((255 - brightness).shl(24))
                }
            }
        }

    fun onStationChanged(station: NearStation) = synchronized(this) {
        detectedTime = SystemClock.elapsedRealtime()
        nearestStation = station
        if (screen) {
            onNotifyStation(station, !keepNotification)
        } else if (forceNotify) {
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK.or(PowerManager.ACQUIRE_CAUSES_WAKEUP),
                "station-found:"
            )
            wakeLock.acquire(10)
            backScreen.visibility = View.VISIBLE
            onNotifyStation(station, false)
        } else {
            requestedStation = station
        }
    }

    fun onLocationChanged(station: NearStation) {
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
            schedule(object : TimerTask() {
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

            }, 0, 1000)
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
            animDisappear.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    notification.visibility = View.GONE
                    animation?.setAnimationListener(null)
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }
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
            animClose.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    icon.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animation?) {
                    notification.visibility = View.GONE
                    animation?.setAnimationListener(null)
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }

            })
            notificationContent.startAnimation(animClose)
        } else {
            notification.visibility = View.VISIBLE
            notificationContent.visibility = View.VISIBLE
            animOpen.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    icon.visibility = View.GONE
                }

                override fun onAnimationEnd(animation: Animation?) {
                    animation?.setAnimationListener(null)
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }

            })
            notificationContent.startAnimation(animOpen)
        }
    }

    fun release() {
        windowManager.removeView(icon)
        windowManager.removeView(backScreen)
        windowManager.removeView(notification)
        nightMode = false

        icon.setOnClickListener(null)
        backScreen.setOnClickListener(null)
        notification.setOnClickListener(null)

        elapsedTimer?.cancel()
        elapsedTimer = null
        durationCallback?.let {
            main.removeCallbacks(it)
            durationCallback = null
        }
    }

}
