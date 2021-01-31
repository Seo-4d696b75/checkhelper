package jp.seo.station.ekisagasu.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.search.formatDistance
import jp.seo.station.ekisagasu.ui.NotificationViewHolder
import jp.seo.station.ekisagasu.ui.OverlayViewHolder
import jp.seo.station.ekisagasu.utils.CurrentLocation
import jp.seo.station.ekisagasu.utils.combineLiveData
import jp.seo.station.ekisagasu.utils.onChanged
import jp.seo.station.ekisagasu.viewmodel.ApplicationViewModel
import kotlinx.coroutines.*
import java.util.*
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
        viewModel.message("onBind: client requests to bind service")
        return StationServiceBinder()
    }


    override fun onUnbind(intent: Intent?): Boolean {
        viewModel.message("onUnbind: client unbinds service")
        return true
    }

    override fun onRebind(intent: Intent?) {
        viewModel.message("onRebind: client binds service again")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        viewModel.message("service received start-command")

        intent?.let {
            if (it.getBooleanExtra(KEY_CLOSE_NOTIFICATION_PANEL, false)) {
                sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            }
            if (it.hasExtra(KEY_REQUEST)) {
                when (it.getStringExtra(KEY_REQUEST)) {
                    REQUEST_EXIT_SERVICE -> {
                        viewModel.setSearchState(false)
                        viewModel.finish()
                    }
                    REQUEST_START_TIMER -> {

                    }
                    REQUEST_FINISH_TIMER -> {

                    }
                    else -> {
                        error(
                            "unknown intent extra. key:KEY_REQUEST value:" + it.getStringExtra(
                                KEY_REQUEST
                            )
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
        viewModel.onServiceInit(this, prefectureRepository)

        viewModel.isServiceAlive = true

        // start this service as foreground one
        startForeground(NotificationViewHolder.NOTIFICATION_TAG, notificationHolder.notification)
        notificationHolder.update("init", "initializing app")

        // init vibrator
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        // when current location changed
        gpsClient.currentLocation.observe(this) {
            it?.let { location ->
                viewModel.location(location)
            }
        }

        // when message from gps
        gpsClient.messageLog.observe(this) {
            it?.let { log ->
                viewModel.message(log)
                gpsClient.messageLog.value = null
            }
        }
        gpsClient.messageError.observe(this) {
            it?.let { mes ->
                viewModel.error(mes)
                gpsClient.messageError.value = null
            }
        }

        // when current location & user setting changed
        combineLiveData<CurrentLocation, Location?, Int>(
            CurrentLocation(null, 1),
            gpsClient.currentLocation,
            userRepository.searchK
        ) { location, k -> CurrentLocation(location, k) }
            .observe(this) { pos ->
                pos.location?.let { loc ->
                    viewModel.updateStation(loc, pos.k)
                }
            }

        // update notification when nearest station changed
        stationRepository.detectedStation.observe(this) {
            it?.let { n ->
                viewModel.logStation(n.station)
                overlayView.onStationChanged(n)
                vibrate(n.station)
            }
        }
        viewModel.selectedLine.observe(this) {
            currentLine = it
        }

        // update notification when nearest station or distance changed
        stationRepository.nearestStation.observe(this) {
            it?.let { s ->
                notificationHolder.update(
                    String.format("%s  %s", s.station.name, s.getDetectedTime()),
                    String.format("%s   %s", formatDistance(s.distance), s.getLinesName())
                )
                overlayView.onLocationChanged(s)
            }
        }

        // update running state
        viewModel.isRunning.onChanged(this) {
            if (it) {

                viewModel.message("start: try to getting GPS ready")

                notificationHolder.update(
                    getString(R.string.notification_title_start),
                    getString(R.string.notification_message_start)
                )
            } else {
                // TODO stop prediction
                Toast.makeText(this, "Stop Search", Toast.LENGTH_SHORT).show()
                viewModel.message("GPS search stopped")
                notificationHolder.update(
                    getString(R.string.notification_title_wait),
                    getString(R.string.notification_message_wait)
                )
            }
        }

        // when finish requested
        viewModel.requestFinishService.observe(this) {
            if (it) {
                viewModel.requestFinishService.value = false
                stopSelf()
            }
        }

        // update user setting
        userRepository.isKeepNotification.observe(this) {
            overlayView.keepNotification = it
        }
        userRepository.brightness.observe(this) {
            overlayView.brightness = it
        }
        userRepository.isNotifyForce.observe(this) {
            overlayView.forceNotify = it
        }
        userRepository.isNotifyPrefecture.observe(this) {
            overlayView.displayPrefecture = it
        }

        // init notification
        notificationHolder.update(
            getString(R.string.notification_title_wait),
            getString(R.string.notification_message_wait)
        )

        // register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(receiver, filter)

        // when user setting changed
        userRepository.isNotify.observe(this) {
            overlayView.notify = it
        }
        userRepository.isKeepNotification.observe(this) {
            overlayView.keepNotification = it
        }
        userRepository.isNotifyForce.observe(this) {
            overlayView.forceNotify = it
        }
        userRepository.gpsUpdateInterval.observe(this) { interval ->
            gpsClient.isRunning.value?.let {
                if (it) gpsClient.requestGPSUpdate(interval, "main-service")
            }
        }
        userRepository.isNotifyPrefecture.observe(this) {
            overlayView.displayPrefecture = it
        }
        userRepository.nightMode.observe(this) {
            overlayView.nightMode = it
        }
        userRepository.brightness.observe(this) {
            overlayView.brightness = it
        }
        userRepository.isVibrate.observe(this) {
            isVibrate = it
        }

        // check notification channel setting
        if (notificationHolder.needNotificationSetting) {
            Log.d("Notification", "needs setting")
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let {
                when (it) {
                    Intent.ACTION_SCREEN_OFF -> {
                        overlayView.screen = false
                        viewModel.message("screen off")
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        viewModel.message("screen on")
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        viewModel.message("user present")
                        overlayView.screen = true
                    }
                }
            }
        }

    }

    @Inject
    lateinit var stationRepository: StationRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefectureRepository: PrefectureRepository

    @Inject
    lateinit var gpsClient: GPSClient

    @Inject
    lateinit var singletonStore: ViewModelStore

    @Inject
    lateinit var mainHandler: Handler

    private val viewModel: ApplicationViewModel by lazy {
        val owner = ViewModelStoreOwner { singletonStore }
        ApplicationViewModel.getInstance(owner, stationRepository, userRepository, gpsClient)
    }

    private val notificationHolder: NotificationViewHolder by lazy {
        NotificationViewHolder(this)
    }

    private val overlayView: OverlayViewHolder by lazy {
        OverlayViewHolder(this, prefectureRepository, mainHandler)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.message("service terminated")
        viewModel.setSearchState(false)
        viewModel.isServiceAlive = false
        viewModel.onServiceFinish(this)
        overlayView.release()
        unregisterReceiver(receiver)
    }

    companion object {
        const val KEY_CLOSE_NOTIFICATION_PANEL = "close_notification_panel"
        const val KEY_REQUEST = "service_request"
        const val REQUEST_EXIT_SERVICE = "exit_service"
        const val REQUEST_START_TIMER = "start_timer"
        const val REQUEST_FINISH_TIMER = "finish_timer"
    }

    private lateinit var vibrator: Vibrator
    private var isVibrate: Boolean = false

    private val VIBRATE_PATTERN_NORMAL = longArrayOf(0, 500, 100, 100)
    private val VIBRATE_PATTERN_ALERT = longArrayOf(0, 500, 100, 100, 100, 100, 100, 100)
    private val VIBRATE_PATTERN_APPROACH = longArrayOf(0, 100, 100, 100, 100, 100)

    private var currentLine: Line? = null

    private fun vibrate(s: Station) {
        val line = currentLine
        if (line != null && s.isLine(line)) {
            vibrate(VIBRATE_PATTERN_ALERT)
        } else {
            vibrate(VIBRATE_PATTERN_NORMAL)
        }
    }

    private fun vibrate(pattern: LongArray) {
        if (!isVibrate || !vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

}
