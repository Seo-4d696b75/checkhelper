package com.seo4d696b75.android.ekisagasu.ui.navigator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorState
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class NavigatorViewModel(
    private val navigatorRepository: NavigatorRepository,
    private val searchRepository: StationSearchRepository,
) : ViewModel() {

    val isVisible = navigatorRepository
        .state
        .onEach { Timber.d("navigator state ${it.javaClass}") }
        .map { it is NavigatorState.Running }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            false,
        )

    private val state = navigatorRepository
        .state
        .mapLatest { state ->
            when (state) {
                NavigatorState.Idle -> DisplayedNavigatorState.Idle
                is NavigatorState.Initializing -> DisplayedNavigatorState.Initializing(state.line)
                is NavigatorState.Result -> DisplayedNavigatorState.Result(
                    line = state.line,
                    stations = buildList {
                        // 最初の要素は現在駅
                        add(
                            DisplayedNavigatorStationState.Current(
                                station = state.current,
                            )
                        )
                        // 続いて予測駅を順に追加する
                        state.predictions.forEach { p ->
                            // 駅の重複無しを確認
                            if (none { it.station == p.station }) {
                                val prediction = DisplayedNavigatorStationState.Prediction(
                                    station = p.station,
                                    distance = p.distance,
                                )
                                add(prediction)
                            }
                        }
                    },
                )
            }
        }

    private val isExpandedFromUser = MutableStateFlow(true)

    val isExpanded = isVisible
        .transformLatest { visible ->
            if (visible) {
                isExpandedFromUser.update { true }
            }
            emitAll(isExpandedFromUser)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            true,
        )

    val uiState = combine(
        isVisible,
        isExpanded,
        state,
    ) { visible, expanded, navigation ->
        NavigatorUiState(
            visible = visible,
            isExpanded = expanded,
            navigation = navigation,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        NavigatorUiState.Initial,
    )

    fun onToggle() {
        isExpandedFromUser.update { !it }
    }

    fun onStopNavigator() {
        searchRepository.clearLine()
        navigatorRepository.stop()
    }
}
