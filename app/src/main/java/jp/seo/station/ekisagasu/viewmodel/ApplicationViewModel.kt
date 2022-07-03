package jp.seo.station.ekisagasu.viewmodel

import android.content.Context
import android.location.Location
import androidx.annotation.MainThread
import androidx.lifecycle.*
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.core.NavigationRepository
import jp.seo.station.ekisagasu.core.PrefectureRepository
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.AppLogger
import jp.seo.station.ekisagasu.repository.LocationRepository
import jp.seo.station.ekisagasu.utils.UnitLiveEvent
import jp.seo.station.ekisagasu.utils.combineLiveData
import jp.seo.station.ekisagasu.utils.getViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter

/**
 * ApplicationComponentでInjectするViewModel(SingletonScoped)
 * @author Seo-4d696b75
 * @version 2021/01/16.
 */
class ApplicationViewModel(
    private val stationRepository: StationRepository,
    private val userRepository: UserRepository,
    private val gps: LocationRepository,
    private val navigator: NavigationRepository,
    private val logger: AppLogger
) : ViewModel() {

    companion object {
        fun getInstance(
            owner: ViewModelStoreOwner,
            stationRepository: StationRepository,
            userRepository: UserRepository,
            gps: LocationRepository,
            navigator: NavigationRepository,
            logger: AppLogger,
        ): ApplicationViewModel {
            return ViewModelProvider(owner, getViewModelFactory {
                ApplicationViewModel(
                    stationRepository, userRepository, gps, navigator, logger,
                )
            }).get(
                ApplicationViewModel::class.java
            )
        }
    }

    var isActivityAlive = false
    var isServiceAlive = false

    val requestFinishActivity = UnitLiveEvent(true)
    val requestFinishService = UnitLiveEvent(true)

    @MainThread
    fun finish() {
        setSearchState(false)
        requestFinishActivity.call()
        requestFinishService.call()

        // this view model is in application-scoped
        // clear variable which is needed to be initialized when activity rebooted
        fixTimer.value = false
        nightMode.value = false
    }

    private var hasAppReboot = false

    /**
     * App起動時の初期化処理
     */
    fun onAppReboot(context: Context) = viewModelScope.launch {
        if (hasAppReboot) return@launch
        userRepository.onAppReboot(context)
        hasAppReboot = true
    }

    var hasPermissionChecked = false
        set(value) {
            if (field) return
            if (!value) throw RuntimeException()
            viewModelScope.launch {
                userRepository.logMessage("all permission checked")
            }
            field = true
        }


    /**
     * 探索が現在進行中であるか
     */
    val isRunning: LiveData<Boolean> = gps.isRunning.asLiveData()


    enum class SearchState {
        STOPPED,
        STARTING,
        RUNNING
    }

    val state: LiveData<SearchState> = combineLiveData(
        SearchState.STOPPED,
        isRunning,
        stationRepository.nearestStation
    ) { run, station ->
        if (run) {
            if (station == null) SearchState.STARTING else SearchState.RUNNING
        } else {
            SearchState.STOPPED
        }
    }

    @MainThread
    fun setSearchState(value: Boolean) {
        if (value) {
            if (stationRepository.dataInitialized) {
                userRepository.gpsUpdateInterval.value?.let {
                    gps.startWatchCurrentLocation(it)
                }
            }

        } else {

            gps.stopWatchCurrentLocation()
            stationRepository.onStopSearch()
            navigator.stop()
        }
    }

    val startTimer = UnitLiveEvent(false)
    val fixTimer = MutableLiveData(false)
    val nightMode = MutableLiveData(false)

    val isNavigationRunning = navigator.running

    fun setNavigationLine(line: Line?) {
        selectLine(line)
        if (line == null) {
            navigator.stop()
        } else {
            navigator.start(line)
        }
    }

    val selectedLine = stationRepository.selectedLine

    fun selectLine(line: Line?) {
        if (isRunning.value == true) {
            stationRepository.selectLine(line)
        }
    }

    fun onServiceInit(context: Context, prefectureRepository: PrefectureRepository) {
        viewModelScope.launch(Dispatchers.IO) {
            //userRepository.onAppReboot(context)
            prefectureRepository.setData(context)
        }
    }

    val appMessage = logger.message

    fun message(text: String) = viewModelScope.launch {
        logger.log(text)
    }

    fun error(text: String) = viewModelScope.launch {
        logger.error(text, text)
    }

    fun saveMessage(message: AppMessage) =
        viewModelScope.launch(Dispatchers.IO) {
            when (message) {
                is AppMessage.AppLog -> userRepository.logMessage(message.message)
                is AppMessage.AppError -> {
                    val str = if (message.cause == null) {
                        message.message
                    } else {
                        val sw = StringWriter()
                        val pw = PrintWriter(sw)
                        message.cause.printStackTrace(pw)
                        String.format("%s caused by;\n%s", message.message, sw.toString())
                    }
                    userRepository.logError(str)
                }
                is AppMessage.AppResolvableException -> userRepository.logError(message.exception.toString())
            }
        }

    fun updateLocation(location: Location) = viewModelScope.launch {
        userRepository.logLocation(location.latitude, location.longitude)
        stationRepository.updateNearestStations(location)
        stationRepository.nearestStation.value?.let {
            navigator.updateLocation(location, it.station)
        }
    }

    fun setSearchK(k: Int) = viewModelScope.launch {
        stationRepository.setSearchK(k)
    }

    fun setSearchInterval(sec: Int) {
        if (isRunning.value == true) {
            gps.startWatchCurrentLocation(sec)
        }
    }

    fun logStation(station: Station) = viewModelScope.launch {
        userRepository.logStation(String.format("%s(%d)", station.name, station.code))
    }


    fun onServiceFinish(context: Context) = viewModelScope.launch {
        userRepository.onAppFinish(context)
    }

}
