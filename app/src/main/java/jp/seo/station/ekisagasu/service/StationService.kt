package jp.seo.station.ekisagasu.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.widget.Toast
import androidx.core.os.HandlerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.seo4d696b75.android.ekisagasu.data.kdtree.formatDistance
import com.seo4d696b75.android.ekisagasu.domain.coroutine.ExternalScope
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.PrefectureRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.ui.MainActivity
import jp.seo.station.ekisagasu.ui.overlay.NotificationViewHolder
import jp.seo.station.ekisagasu.ui.overlay.OverlayViewHolder
import jp.seo.station.ekisagasu.ui.overlay.WakeupActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.util.Calendar
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
                        viewModel.requestAppFinish()
                    }

                    REQUEST_START_TIMER -> {
                        setTimer()
                    }

                    REQUEST_FINISH_TIMER -> {
                        timerRunning = false
                        overlayView.setTimerState(false)
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
        // init repository
        viewModel.onServiceInit()

        // start this service as foreground one
        startForeground(NotificationViewHolder.NOTIFICATION_TAG, notificationHolder.notification)
        notificationHolder.update("init", "initializing app")

        // init vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VIBRATOR_MANAGER_SERVICE)?.let {
                val manager = it as VibratorManager
                manager.defaultVibrator
            }
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        // update notification when nearest station changed
        viewModel.detectedStation
            .flowWithLifecycle(lifecycle)
            .onEach { n ->
                overlayView.onStationChanged(n)
                vibrate(n.station)
            }
            .launchIn(lifecycleScope)

        viewModel.selectedLine
            .flowWithLifecycle(lifecycle)
            .onEach { currentLine = it }
            .launchIn(lifecycleScope)

        // update notification when nearest station or distance changed
        viewModel.nearestStation
            .flowWithLifecycle(lifecycle)
            .onEach { s ->
                notificationHolder.update(
                    String.format("%s  %s", s.station.name, s.getDetectedTime()),
                    String.format("%s   %s", s.distance.formatDistance, s.getLinesName()),
                )
                overlayView.onLocationChanged(s)
            }
            .launchIn(lifecycleScope)

        // update running state
        viewModel.isRunning
            .flowWithLifecycle(lifecycle)
            .drop(1)
            .onEach {
                if (it) {
                    notificationHolder.update(
                        getString(R.string.notification_title_start),
                        getString(R.string.notification_message_start),
                    )
                } else {
                    Toast.makeText(
                        this@StationService,
                        getString(R.string.message_stop_search),
                        Toast.LENGTH_SHORT,
                    ).show()
                    notificationHolder.update(
                        getString(R.string.notification_title_wait),
                        getString(R.string.notification_message_wait),
                    )
                }
                overlayView.isSearchRunning = it
            }
            .launchIn(lifecycleScope)

        // when navigation changed
        viewModel.isNavigatorRunning
            .flowWithLifecycle(lifecycle)
            .onEach {
                if (it) {
                    hasApproach = false
                    nextApproachStation = null
                    val line = viewModel.navigatorLine ?: return@onEach
                    val n = viewModel.nearestStation.first()
                    overlayView.navigation.startNavigation(line, n.station)
                } else {
                    overlayView.navigation.stopNavigation()
                }
                overlayView.isNavigationRunning = it
            }
            .launchIn(lifecycleScope)

        viewModel.navigationPrediction
            .flowWithLifecycle(lifecycle)
            .onEach { result ->
                if (result.size > 0) {
                    val next = result.getStation(0)
                    if (nextApproachStation == null || nextApproachStation != next) {
                        nextApproachStation = next
                        hasApproach = false
                    } else if (
                        isVibrateWhenApproach && !hasApproach && result.getDistance(0) < vibrateMeterWhenApproach
                    ) {
                        hasApproach = true
                        vibrate(VIBRATE_PATTERN_APPROACH)
                    }
                }
                overlayView.navigation.onUpdate(result)
            }
            .launchIn(lifecycleScope)

        // when finish requested
        viewModel.appFinish
            .flowWithLifecycle(lifecycle)
            .onEach { stopService() }
            .launchIn(lifecycleScope)

        // init notification
        notificationHolder.update(
            getString(R.string.notification_title_wait),
            getString(R.string.notification_message_wait),
        )

        // register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(receiver, filter)

        // when user setting changed
        viewModel.userSetting
            .flowWithLifecycle(lifecycle)
            .onEach {
                // station search param
                viewModel.setSearchK(it.searchK)
                viewModel.setSearchInterval(it.locationUpdateInterval)
                // notification param
                overlayView.apply {
                    notify = it.isPushNotification
                    keepNotification = it.isKeepNotification
                    forceNotify = it.isPushNotificationForce
                    displayPrefecture = it.isShowPrefectureNotification
                    nightModeTimeout = it.nightModeTimeout
                    brightness = it.nightModeBrightness
                    timerPosition = it.timerPosition
                }
                // vibration param
                isVibrate = it.isVibrate
                isVibrateWhenApproach = it.isVibrateWhenApproach
                vibrateMeterWhenApproach = it.vibrateDistance
            }
            .launchIn(lifecycleScope)

        // switch night mode
        viewModel.nightMode
            .flowWithLifecycle(lifecycle)
            .onEach { overlayView.nightMode = it }
            .launchIn(lifecycleScope)

        // set timer
        overlayView.timerListener = { setTimer() }

        viewModel.startTimer
            .flowWithLifecycle(lifecycle)
            .onEach { setTimer() }
            .launchIn(lifecycleScope)

        viewModel.fixTimer
            .flowWithLifecycle(lifecycle)
            .onEach { overlayView.fixTimer(it) }
            .launchIn(lifecycleScope)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?,
        ) {
            intent?.action?.let {
                when (it) {
                    Intent.ACTION_SCREEN_OFF -> {
                        overlayView.screen = false
                    }

                    Intent.ACTION_USER_PRESENT -> {
                        overlayView.screen = true
                    }

                    else -> {}
                }
            }
        }
    }

    @Inject
    lateinit var prefectureRepository: PrefectureRepository

    @Inject
    lateinit var viewModel: ServiceViewModel

    @Inject
    @ExternalScope
    lateinit var job: Job

    private val notificationHolder: NotificationViewHolder by lazy {
        NotificationViewHolder(this)
    }

    private val overlayView: OverlayViewHolder by lazy {
        val handler = HandlerCompat.createAsync(Looper.getMainLooper())
        OverlayViewHolder(
            this,
            prefectureRepository,
            handler,
            wakeupCallback = {
                val intent =
                    Intent(this, WakeupActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                startActivity(intent)
            },
            selectLineCallback = {
                val intent =
                    Intent(this, MainActivity::class.java).apply {
                        putExtra(MainActivity.INTENT_KEY_SELECT_NAVIGATION, true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                startActivity(intent)
            },
            stopNavigationCallback = {
                viewModel.clearNavigationLine()
            },
        )
    }

    private fun stopService() {
        viewModel.saveTimerPosition(overlayView.timerPosition)
        overlayView.release()
        unregisterReceiver(receiver)
        stopSelf()
        job.cancel()
    }

    companion object {
        const val KEY_REQUEST = "service_request"
        const val REQUEST_EXIT_SERVICE = "exit_service"
        const val REQUEST_START_TIMER = "start_timer"
        const val REQUEST_FINISH_TIMER = "finish_timer"
        private val VIBRATE_PATTERN_NORMAL = longArrayOf(0, 500, 100, 100)
        private val VIBRATE_PATTERN_ALERT = longArrayOf(0, 500, 100, 100, 100, 100, 100, 100)
        private val VIBRATE_PATTERN_APPROACH = longArrayOf(0, 100, 100, 100, 100, 100)
    }

    private var vibrator: Vibrator? = null
    private var isVibrate: Boolean = false
    private var isVibrateWhenApproach: Boolean = false

    private var currentLine: Line? = null
    private var vibrateMeterWhenApproach: Int = 100
    private var hasApproach: Boolean = false
    private var nextApproachStation: Station? = null

    private fun vibrate(s: Station) {
        val line = currentLine
        if (line != null && !s.isLine(line)) {
            vibrate(VIBRATE_PATTERN_ALERT)
        } else {
            vibrate(VIBRATE_PATTERN_NORMAL)
        }
    }

    private fun vibrate(pattern: LongArray) {
        if (!isVibrate || vibrator?.hasVibrator() != true) return
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private var timerRunning = false

    private fun setTimer() {
        if (timerRunning) {
            overlayView.setTimerState(true)
            Toast.makeText(this, getString(R.string.timer_wait_message), Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_MESSAGE, getString(R.string.timer_title))
            .putExtra(AlarmClock.EXTRA_LENGTH, 300)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val manager = getSystemService(ALARM_SERVICE)
        if (manager is AlarmManager && intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            overlayView.setTimerState(true)
            val calendar = Calendar.getInstance().apply {
                add(Calendar.MINUTE, 5)
            }
            val pending = PendingIntent.getService(
                applicationContext,
                5,
                Intent(applicationContext, StationService::class.java).putExtra(
                    KEY_REQUEST,
                    REQUEST_FINISH_TIMER,
                ),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            manager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pending)
            timerRunning = true
            Toast.makeText(this, getString(R.string.timer_set_message), Toast.LENGTH_SHORT).show()
        } else {
            Timber.d("Failed to set timer")
        }
    }
}
