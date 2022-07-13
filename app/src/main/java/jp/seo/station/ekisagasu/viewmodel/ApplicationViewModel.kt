package jp.seo.station.ekisagasu.viewmodel

import android.content.Context
import androidx.annotation.MainThread
import androidx.lifecycle.*
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.repository.NavigationRepository
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.repository.AppLogger
import jp.seo.station.ekisagasu.repository.LocationRepository
import jp.seo.station.ekisagasu.utils.combineLiveData
import jp.seo.station.ekisagasu.utils.getViewModelFactory
import kotlinx.coroutines.launch

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

    val appMessage = logger.message

    fun message(text: String) = viewModelScope.launch {
        logger.log(text)
    }

    fun error(text: String) = viewModelScope.launch {
        logger.error(text, text)
    }
}
