package com.seo4d696b75.android.ekisagasu.ui.navigator

import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorState
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

class GetNavigatorUiStateUseCase @Inject constructor(
    private val navigator: NavigatorRepository,
    private val dataRepository: DataRepository,
) {
    operator fun invoke() = navigator
        .state
        .mapLatest { state ->
            when (state) {
                null -> NavigatorUiState.Idle
                is NavigatorState.Initializing -> NavigatorUiState.Initializing(state.line)
                is NavigatorState.Result -> NavigatorUiState.Result(
                    line = state.line,
                    stations = buildList {
                        // 最初の要素は現在駅
                        add(
                            NavigatorStationUiState.Current(
                                station = state.current,
                                lines = dataRepository
                                    .getLines(state.current.lines)
                                    .map {
                                        NavigatorLineUiState(
                                            line = it,
                                            isCurrentSelected = it.id == state.line.id,
                                        )
                                    },
                            )
                        )
                        // 続いて予測駅を順に追加する
                        state.predictions.forEach { p ->
                            // 駅の重複無しを確認
                            if (none { it.station == p.station }) {
                                val prediction = NavigatorStationUiState.Prediction(
                                    station = p.station,
                                    distance = p.distance,
                                    lines = dataRepository
                                        .getLines(p.station.lines)
                                        .map {
                                            NavigatorLineUiState(
                                                line = it,
                                                isCurrentSelected = it.id == state.line.id,
                                            )
                                        }
                                )
                                add(prediction)
                            }
                        }
                    },
                )
            }
        }
}

sealed interface NavigatorUiState {
    data object Idle : NavigatorUiState
    data class Initializing(
        val line: Line,
    ) : NavigatorUiState

    data class Result(
        val line: Line,
        val stations: List<NavigatorStationUiState>
    ) : NavigatorUiState
}

// ListViewでまとめてUI表示するため、現在駅と予測駅を同じ型で扱う必要がある
sealed interface NavigatorStationUiState {
    val station: Station
    val lines: List<NavigatorLineUiState>

    data class Current(
        override val station: Station,
        override val lines: List<NavigatorLineUiState>,
    ) : NavigatorStationUiState

    data class Prediction(
        override val station: Station,
        override val lines: List<NavigatorLineUiState>,
        val distance: Float,
    ) : NavigatorStationUiState
}

data class NavigatorLineUiState(
    val line: Line,
    val isCurrentSelected: Boolean,
)
