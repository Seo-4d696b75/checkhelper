package jp.seo.station.ekisagasu.core

import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.gms.common.api.ResolvableApiException
import dagger.hilt.android.AndroidEntryPoint
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.ui.NotificationViewHolder
import jp.seo.station.ekisagasu.utils.CurrentLocation
import jp.seo.station.ekisagasu.utils.combineLiveData
import jp.seo.station.ekisagasu.viewmodel.ApplicationViewModel
import kotlinx.coroutines.*
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 *
 * Main service providing core function in background.
 * This service has to sense GPS location, so must be run as foreground service.
 */
@AndroidEntryPoint
class StationService : LifecycleService(), CoroutineScope {

    @Inject
    lateinit var singletonStore: ViewModelStore

    private val viewModel: ApplicationViewModel by lazy {
        val owner = ViewModelStoreOwner { singletonStore }
        ApplicationViewModel.getInstance(owner)
    }

    private var bindActivity = false

    inner class StationServiceBinder : Binder() {
        fun bind(): StationService {
            bindActivity = true
            return this@StationService
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        message("onBind: client requests to bind service")
        return StationServiceBinder()
    }


    override fun onUnbind(intent: Intent?): Boolean {
        bindActivity = false
        message("onUnbind: client unbinds service")
        return true
    }

    override fun onRebind(intent: Intent?) {
        message("onRebind: client binds service again")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        message("service received start-command")
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
                        stop()
                        if (bindActivity) {
                            viewModel.requestFinish.postValue(true)
                        } else {
                            stopSelf()
                        }
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
        launch {
            userRepository.onAppReboot()
            prefectureRepository.setData(this@StationService)
        }

        // start this service as foreground one
        startForeground(NotificationViewHolder.NOTIFICATION_TAG, notificationHolder.notification)
        notificationHolder.update("init", "initializing app")

        // when current location & user setting changed
        combineLiveData<CurrentLocation, Location?, Int>(
            CurrentLocation(null, 1),
            gpsClient.currentLocation,
            userRepository.searchK
        ) { location, k -> CurrentLocation(location, k) }
            .observe(this) { pos ->
                pos.location?.let { loc ->
                    launch {
                        stationRepository.updateNearestStations(loc, pos.k)
                            ?.let {
                                userRepository.logStation(String.format("%s(%d)", it.name, it.code))

                            }
                    }
                }
            }

        // update notification when nearest station changed
        stationRepository.nearestStation.observe(this) {
            it?.let { s ->
                notificationHolder.update(
                    String.format("%s  %s", s.station.name, s.getDetectedTime()),
                    String.format("%s   %s", s.distance, s.getLinesName())
                )

            }
        }

        viewModel.isRequestRunning.observe(this) {
            if (it) {
                start()
            } else {
                stop()
            }
        }

    }


    private val serviceJob = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + serviceJob

    @Inject
    lateinit var stationRepository: StationRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefectureRepository: PrefectureRepository

    val notificationHolder: NotificationViewHolder by lazy {
        NotificationViewHolder(this)
    }

    val gpsClient: GPSClient by lazy {
        GPSClient(this, object : GPSCallback {
            override fun onLocationUpdated(location: Location) {
                launch {
                    userRepository.logLocation(location.latitude, location.longitude)
                }
            }

            override fun onGPSStop(mes: String) {
                error(mes)
            }

            override fun onGPSLog(log: String) {
                message(log)
            }

        })
    }

    fun message(mes: String) {
        launch { userRepository.logMessage(mes) }
    }

    fun error(mes: String, cause: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        cause.printStackTrace(pw)
        error(String.format("%s caused by;\n%s", mes, sw.toString()))
    }

    fun error(mes: String) {
        launch {
            userRepository.logError(mes)
            withContext(Dispatchers.Main) { stop() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        message("service terminated")

        if (userRepository.hasError) {
            userRepository.writeErrorLog(getString(R.string.app_name), getExternalFilesDir(null))
        }
        gpsClient.release()
    }

    private var running = false

    /**
     * start GPS tracking and request updating the nearest station etc.
     */
    @MainThread
    private fun start() {
        if (running || !stationRepository.dataInitialized) {
            return
        }
        // TODO init pop-up notification
        // TODO update notification
        message("start: try to getting GPS ready")
        // TODO store user setting values in userRepository
        try {
            gpsClient.requestGPSUpdate(5, "main-service")
            notificationHolder.update(
                getString(R.string.notification_title_start),
                getString(R.string.notification_message_start)
            )
            viewModel.setSearchState(true)
        } catch (e: ResolvableApiException) {
            viewModel.onApiException(e)
            viewModel.setSearchState(false)
        }
    }

    @MainThread
    private fun stop() {
        if (!running) return
        stationRepository.onStopSearch()
        // TODO update (pop-up) notification
        gpsClient.stopGPSUpdate("main-service")
        // TODO stop prediction
        Toast.makeText(this, "Stop Search", Toast.LENGTH_SHORT).show()
        message("GPS search stopped")
        notificationHolder.update(
            getString(R.string.notification_title_wait),
            getString(R.string.notification_message_wait)
        )
        viewModel.setSearchState(false)
    }

    companion object {
        const val KEY_CLOSE_NOTIFICATION_PANEL = "close_notification_panel"
        const val KEY_REQUEST = "service_request"
        const val REQUEST_EXIT_SERVICE = "exit_service"
        const val REQUEST_START_TIMER = "start_timer"
        const val REQUEST_FINISH_TIMER = "finish_timer"
    }

}
