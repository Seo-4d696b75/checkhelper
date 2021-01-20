package jp.seo.station.ekisagasu.core

import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
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

        //TODO only for the first time
        viewModel.message("service received start-command")
        notificationHolder.update(
            getString(R.string.notification_title_wait),
            getString(R.string.notification_message_wait)
        )

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
            }
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

                // TODO init pop-up notification
                viewModel.message("start: try to getting GPS ready")
                // TODO store user setting values in userRepository

                notificationHolder.update(
                    getString(R.string.notification_title_start),
                    getString(R.string.notification_message_start)
                )
            } else {
                // TODO update (pop-up) notification
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
        overlayView.release()
        if (userRepository.hasError) {
            userRepository.writeErrorLog(getString(R.string.app_name), getExternalFilesDir(null))
        }
    }

    companion object {
        const val KEY_CLOSE_NOTIFICATION_PANEL = "close_notification_panel"
        const val KEY_REQUEST = "service_request"
        const val REQUEST_EXIT_SERVICE = "exit_service"
        const val REQUEST_START_TIMER = "start_timer"
        const val REQUEST_FINISH_TIMER = "finish_timer"
    }

}
