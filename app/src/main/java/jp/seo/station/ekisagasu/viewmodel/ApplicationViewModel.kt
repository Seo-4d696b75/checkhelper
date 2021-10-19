package jp.seo.station.ekisagasu.viewmodel

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.core.*
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
    private val gps: GPSClient,
    private val navigator: NavigationRepository,
) : ViewModel() {

    companion object {
        fun getInstance(
            owner: ViewModelStoreOwner,
            stationRepository: StationRepository,
            userRepository: UserRepository,
            gps: GPSClient,
            navigator: NavigationRepository
        ): ApplicationViewModel {
            return ViewModelProvider(owner, getViewModelFactory {
                ApplicationViewModel(stationRepository, userRepository, gps, navigator)
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

    var hasPermissionChecked = false
        set(value) {
            if (value) return
            if (!value) throw RuntimeException()
            viewModelScope.launch {
                userRepository.logMessage("all permission checked")
            }
            field = true
        }

    fun startService(activity: AppCompatActivity) {
        if (!isServiceAlive && hasPermissionChecked) {

            val intent = Intent(activity, StationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(intent)
            } else {
                activity.startService(intent)
            }
            viewModelScope.launch(Dispatchers.IO) {
                userRepository.onAppReboot(activity)
            }
            isServiceAlive = true
        }
    }

    /**
     * 探索が現在進行中であるか
     */
    val isRunning: LiveData<Boolean> = gps.isRunning


    enum class SearchState {
        STOPPED,
        STARTING,
        RUNNING
    }

    val state: LiveData<SearchState> = combineLiveData(
        SearchState.STOPPED,
        gps.isRunning,
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
                    gps.requestGPSUpdate(it)
                }
            }

        } else {

            gps.stopGPSUpdate()
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
            message(String.format("select line: %s", line?.name ?: "null"))
        }
    }

    val apiException = gps.apiException

    fun onServiceInit(context: Context, prefectureRepository: PrefectureRepository) {
        viewModelScope.launch(Dispatchers.IO) {
            //userRepository.onAppReboot(context)
            prefectureRepository.setData(context)
        }
    }

    fun message(mes: String) {
        viewModelScope.launch { userRepository.logMessage(mes) }
    }

    fun error(mes: String, cause: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        cause.printStackTrace(pw)
        error(String.format("%s caused by;\n%s", mes, sw.toString()))
    }

    fun error(mes: String) {
        viewModelScope.launch {
            userRepository.logError(mes)
            setSearchState(false)
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
            gps.requestGPSUpdate(sec)
        }
    }

    fun logStation(station: Station) = viewModelScope.launch {
        userRepository.logStation(String.format("%s(%d)", station.name, station.code))
    }


    fun onServiceFinish(context: Context) = viewModelScope.launch {
        userRepository.onAppFinish(context)
    }

}
