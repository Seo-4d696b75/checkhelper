package jp.seo.station.ekisagasu.core

import android.app.Activity
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.lifecycle.*
import com.google.android.gms.common.api.ResolvableApiException
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.search.KdTree
import jp.seo.station.ekisagasu.ui.NotificationViewHolder
import jp.seo.station.ekisagasu.utils.CurrentLocation
import jp.seo.station.ekisagasu.utils.combine
import kotlinx.coroutines.*
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.coroutines.CoroutineContext

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 *
 * Main service providing core function in background.
 * This service has to sense GPS location, so must be run as foreground service.
 */
class StationService : LifecycleService(), CoroutineScope {

    private var activity: Activity? = null

    inner class StationServiceBinder : Binder() {
        fun bind(a: Activity): StationService {
            activity = a
            return this@StationService
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        message("onBind: client requests to bind service")
        return StationServiceBinder()
    }


    override fun onUnbind(intent: Intent?): Boolean {
        activity = null
        message("onUnbind: client unbinds service")
        return true
    }

    override fun onRebind(intent: Intent?) {
        message("onRebind: client binds service again")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        message("service received start-command")

        intent?.let {
            if (it.getBooleanExtra(KEY_CLOSE_NOTIFICATION_PANEL, false)) {
                sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            }
            if (it.hasExtra(KEY_REQUEST)) {
                when (it.getStringExtra(KEY_REQUEST)) {
                    REQUEST_EXIT_SERVICE -> {
                        stop()
                        activity?.also { a -> a.finish() } ?: run { stopSelf() }
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
        }

        // start this service as foreground one
        startForeground(NotificationViewHolder.NOTIFICATION_TAG, notificationHolder.notification)
        notificationHolder.update("init", "initializing app")

        // when current location & user setting changed
        combine<CurrentLocation, Location?, Int>(
            CurrentLocation(null, 1),
            gpsClient.currentLocation,
            userRepository.searchK
        ) { location, k -> CurrentLocation(location, k) }
            .observe(this) { pos ->
                pos.location?.let { loc ->
                    launch {
                        stationRepository.updateNearestStations(loc.latitude, loc.longitude, pos.k)
                            ?.let {
                                userRepository.logStation(String.format("%s(%d)", it.name, it.code))
                            }
                    }
                }
            }
    }


    private val serviceJob = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + serviceJob

    val userRepository: UserRepository by lazy {
        val dao = getUserDatabase(this).userDao
        UserRepository(dao)
    }

    val stationRepository: StationRepository by lazy {
        val db = getStationDatabase(this)
        val api = getAPIClient(getString(R.string.api_url_base))
        val tree = KdTree(db.dao)
        StationRepository(db.dao, api, tree)
    }

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
    private val _running = MutableLiveData<Boolean>(false)

    val isRunning: LiveData<Boolean>
        get() = _running

    /**
     * start GPS tracking and request updating the nearest station etc.
     */
    @MainThread
    fun start() {
        if (running || !stationRepository.dataInitialized) return
        // TODO init pop-up notification
        // TODO update notification
        message("start: try to getting GPS ready")
        // TODO store user setting values in userRepository
        try {
            gpsClient.requestGPSUpdate(5, "main-service")
            running = true
            _running.value = true
            notificationHolder.update("start", "searching for nearest stations")
        } catch (e: ResolvableApiException) {
            userRepository.onApiException(e)
        }
    }

    @MainThread
    fun stop() {
        if (running) {
            stationRepository.onStopSearch()
            // TODO update (pop-up) notification
            gpsClient.stopGPSUpdate("main-service")
            // TODO stop prediction
            Toast.makeText(this, "Stop Search", Toast.LENGTH_SHORT).show()
            running = false
            _running.value = false
            message("GPS search stopped")
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
