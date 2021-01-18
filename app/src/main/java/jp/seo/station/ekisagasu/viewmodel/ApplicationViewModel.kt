package jp.seo.station.ekisagasu.viewmodel

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ResolvableApiException
import jp.seo.station.ekisagasu.core.GPSClient
import jp.seo.station.ekisagasu.core.PrefectureRepository
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.core.StationService
import jp.seo.station.ekisagasu.core.UserRepository
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
) : ViewModel() {

    companion object {
        fun getInstance(
            owner: ViewModelStoreOwner,
            stationRepository: StationRepository,
            userRepository: UserRepository,
            gps: GPSClient
        ): ApplicationViewModel {
            return ViewModelProvider(owner, getViewModelFactory {
                ApplicationViewModel(stationRepository, userRepository, gps)
            }).get(
                ApplicationViewModel::class.java
            )
        }
    }

    var isActivityAlive = false
    var isServiceAlive = false
    val requestFinishActivity = MutableLiveData<Boolean>(false)
    val requestFinishService = MutableLiveData<Boolean>(false)

    @MainThread
    fun finish() {
        if (isActivityAlive) requestFinishActivity.value = true
        if (isServiceAlive) requestFinishService.value = true
    }

    fun startService(activity: AppCompatActivity) {
        if (!isServiceAlive) {

            val intent = Intent(activity, StationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(intent)
            } else {
                activity.startService(intent)
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
                gps.requestGPSUpdate(5, "main-service")
            }

        } else {

            if (gps.stopGPSUpdate("main-service")) {
                stationRepository.onStopSearch()
            }
        }
    }

    @MainThread
    fun onResolvedAPIException() = gps.onResolvedAPIException()

    val apiException: LiveData<ResolvableApiException?> = gps.apiException

    fun onServiceInit(context: Context, prefectureRepository: PrefectureRepository) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.onAppReboot()
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

    fun location(location: Location) {
        viewModelScope.launch {
            userRepository.logLocation(location.latitude, location.longitude)
        }
    }

    fun updateStation(location: Location, k: Int) {
        viewModelScope.launch(Dispatchers.IO) {

            stationRepository.updateNearestStations(location, k)
                ?.let {
                    userRepository.logStation(String.format("%s(%d)", it.name, it.code))

                }
        }
    }

}
