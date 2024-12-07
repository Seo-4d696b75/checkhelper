package com.seo4d696b75.android.ekisagasu.ui.popup

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.coroutine.mapWithPrevious
import com.seo4d696b75.android.ekisagasu.domain.dataset.PrefectureRepository
import com.seo4d696b75.android.ekisagasu.domain.screen.PopupStatus
import com.seo4d696b75.android.ekisagasu.domain.screen.PopupStatusRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchState
import com.seo4d696b75.android.ekisagasu.domain.user.UserSettingRepository
import com.seo4d696b75.android.ekisagasu.ui.popup.component.StationDetectedTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class PopupViewModel(
    private val settingRepository: UserSettingRepository,
    private val searchRepository: StationSearchRepository,
    private val prefectureRepository: PrefectureRepository,
    private val popupStatusRepository: PopupStatusRepository,
) : ViewModel() {

    init {
        viewModelScope.launch {
            awaitCancellation()
        }.invokeOnCompletion {
            Timber.d("PopupViewModel viewModelScope cancelled by $it")
        }
    }

    private val status = popupStatusRepository
        .status
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            PopupStatus.Invisible,
        )

    val isVisible = status
        .map { it is PopupStatus.Visible }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            false,
        )

    val isExpanded = status
        .mapWithPrevious { previous, value ->
            when (value) {
                is PopupStatus.Visible -> value.isExpanded
                PopupStatus.Invisible -> if (previous is PopupStatus.Visible) {
                    // アニメーションの自然のため直前の状態を保持する
                    previous.isExpanded
                } else {
                    true
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            true,
        )

    // ポップアップに表示する状態を生成する
    private val stationState: Flow<PopupStationState> = searchRepository
        .state
        .map {
            when (it) {
                is StationSearchState.Result -> it.nearest.station
                else -> null
            }
        }
        .distinctUntilChanged()
        .flatMapLatest { result ->
            if (result == null) {
                flowOf(PopupStationState.None)
            } else {
                combine(
                    searchRepository
                        .state
                        .filterIsInstance<StationSearchState.Result>()
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
                        showPrefecture = showPrefecture,
                    )
                }
            }
        }

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
        when (status.value) {
            is PopupStatus.Visible.Fixed -> popupStatusRepository.togglePopup()
            PopupStatus.Visible.Closable -> popupStatusRepository.closePopup()
            PopupStatus.Invisible -> {}
        }
    }
}
