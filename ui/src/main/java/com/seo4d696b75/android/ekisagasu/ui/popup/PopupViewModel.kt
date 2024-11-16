package com.seo4d696b75.android.ekisagasu.ui.popup

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.dataset.PrefectureRepository
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.location.LocationState
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorState
import com.seo4d696b75.android.ekisagasu.domain.screen.ScreenRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.user.UserSettingRepository
import com.seo4d696b75.android.ekisagasu.ui.popup.component.StationDetectedTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class PopupViewModel(
    private val settingRepository: UserSettingRepository,
    private val searchRepository: StationSearchRepository,
    private val prefectureRepository: PrefectureRepository,
    private val navigatorRepository: NavigatorRepository,
    private val locationRepository: LocationRepository,
    private val screenRepository: ScreenRepository,
) : ViewModel() {

    init {
        viewModelScope.launch {
            awaitCancellation()
        }.invokeOnCompletion {
            Timber.d("PopupViewModel viewModelScope cancelled by $it")
        }
    }

    val isVisible: StateFlow<Boolean> = combine(
        settingRepository.setting,
        locationRepository.currentLocation,
        navigatorRepository.state,
    ) { setting, location, navigation ->
        val enabled = setting.isPushNotification &&
            location is LocationState.Result &&
            navigation !is NavigatorState.Running
        val keep = setting.isKeepNotification
        enabled to keep
    }
        .distinctUntilChanged()
        .flatMapLatest { pair ->
            val (enabled, keep) = pair
            if (!enabled) {
                // 常に非表示
                flowOf(false)
            } else if (keep) {
                // 常に表示
                flowOf(true)
            } else {
                // 状態・ユーザ操作によって表示・非表示が変化する
                isVisibleFromUpstream.transformLatest { visible ->
                    if (visible) {
                        isVisibleFromUser.update { true }
                        emitAll(isVisibleFromUser)
                    } else {
                        emit(false)
                    }
                }
            }
        }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            false,
        )

    // ユーザ操作による状態（優先）
    private val isVisibleFromUser = MutableStateFlow(true)

    // ユーザ操作以外によって決まる状態
    private val isVisibleFromUpstream = channelFlow {
        send(false)
        var forceNotify = false
        var screen = false
        var pending = false
        var timer: Job? = null
        val notify = suspend {
            send(true)
            timer?.cancel()
            timer = launch {
                delay(5000L)
                send(false)
            }
        }
        launch {
            settingRepository
                .setting
                .collect {
                    forceNotify = it.isPushNotificationForce
                }
        }
        launch {
            screenRepository
                .isTurnOn
                .collect {
                    screen = it
                    if (it && pending) {
                        pending = false
                        notify()
                    }
                }
        }
        launch {
            searchRepository
                .result
                .distinctUntilChangedBy { it?.detected?.station }
                .drop(1)
                .filterNotNull()
                .collect {
                    // 最近傍の駅が変化したとき
                    if (screen || forceNotify) {
                        notify()
                    } else {
                        pending = true
                    }
                }
        }
    }

    // ポップアップに表示する状態を生成する
    private val stationState: Flow<PopupStationState> = searchRepository
        .result
        .distinctUntilChangedBy { it?.detected?.station }
        .flatMapLatest { result ->
            if (result == null) {
                flowOf(PopupStationState.None)
            } else {
                combine(
                    searchRepository
                        .result
                        .filterNotNull()
                        .map {
                            // 駅は同じでも距離が変化する
                            it.nearest
                        },
                    flow {
                        // 最近傍の駅が変化した時刻を起点に10秒ごと更新
                        val since = SystemClock.elapsedRealtime()
                        while (true) {
                            val seconds = (SystemClock.elapsedRealtime() - since) / 1000L
                            val time = when {
                                seconds < 10 -> StationDetectedTime.Now
                                seconds < 60 -> StationDetectedTime.Sec
                                else -> StationDetectedTime.Min((seconds / 60L).toInt())
                            }
                            emit(time)
                            delay(10_000L)
                        }
                    }.distinctUntilChanged(),
                    settingRepository
                        .setting
                        .map { it.isShowPrefectureNotification }
                        .distinctUntilChanged(),
                ) { nearest, time, showPrefecture ->
                    PopupStationState.Result(
                        nearest = nearest,
                        time = time,
                        prefecture = if (showPrefecture) {
                            prefectureRepository.getName(nearest.station.prefecture)
                        } else {
                            null
                        },
                    )
                }
            }
        }

    // ポップアップを常に表示する条件下の、ユーザ操作による開閉状態
    private val isExpandedFromUser = MutableStateFlow(true)

    val isExpanded = settingRepository
        .setting
        .map { it.isKeepNotification }
        .distinctUntilChanged()
        .transformLatest { keep ->
            if (keep) {
                isExpandedFromUser.update { true }
                emitAll(isExpandedFromUser)
            } else {
                emit(true)
            }
        }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            true,
        )

    val uiStateFlow = combine(
        isVisible,
        isExpanded,
        stationState,
    ) { visible, expanded, s ->
        PopupUiState(
            visible = visible,
            isExpanded = expanded,
            current = s,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        PopupUiState.Initial,
    )

    fun onClicked() {
        isExpandedFromUser.update { !it }
        isVisibleFromUser.update { false }
    }
}
