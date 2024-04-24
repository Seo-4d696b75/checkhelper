package com.seo4d696b75.android.ekisagasu.ui.navigator

import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorState
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetNavigatorUiStateUseCase @Inject constructor(
    private val navigator: NavigatorRepository,
) {
    operator fun invoke() = navigator
        .state
        .map { state ->
            when (state) {
                null -> NavigatorUiState.Idle
                is NavigatorState.Initializing -> NavigatorUiState.Initializing(state.line)
                is NavigatorState.Result -> NavigatorUiState.Result(
                    line = state.line,
                    stations = buildList {
                        // 最初の要素は現在駅
                        add(NavigatorStationUiState.Current(state.current))
                        // 続いて予測駅を順に追加する
                        state.predictions.forEach {
                            val prediction = NavigatorStationUiState.Prediction(
                                station = it.station,
                                distance = it.distance,
                            )
                            add(prediction)
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

    data class Current(override val station: Station) : NavigatorStationUiState
    data class Prediction(
        override val station: Station,
        val distance: Float,
    ) : NavigatorStationUiState
}
