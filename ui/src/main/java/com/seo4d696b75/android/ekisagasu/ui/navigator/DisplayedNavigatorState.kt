package com.seo4d696b75.android.ekisagasu.ui.navigator

import androidx.compose.runtime.Immutable
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

class GetDisplayedNavigatorStateUseCase @Inject constructor(
    private val navigator: NavigatorRepository,
    private val dataRepository: DataRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke() = navigator
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
                                lines = dataRepository
                                    .getLines(state.current.lines)
                                    .map {
                                        DisplayedNavigatorLineState(
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
                                val prediction = DisplayedNavigatorStationState.Prediction(
                                    station = p.station,
                                    distance = p.distance,
                                    lines = dataRepository
                                        .getLines(p.station.lines)
                                        .map {
                                            DisplayedNavigatorLineState(
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

@Immutable
sealed interface DisplayedNavigatorState {
    val line: Line?

    data object Idle : DisplayedNavigatorState {
        override val line = null
    }

    data class Initializing(
        override val line: Line,
    ) : DisplayedNavigatorState

    data class Result(
        override val line: Line,
        val stations: List<DisplayedNavigatorStationState>
    ) : DisplayedNavigatorState
}

@Immutable
sealed interface DisplayedNavigatorStationState {
    val station: Station
    val lines: List<DisplayedNavigatorLineState>

    data class Current(
        override val station: Station,
        override val lines: List<DisplayedNavigatorLineState>,
    ) : DisplayedNavigatorStationState

    data class Prediction(
        override val station: Station,
        override val lines: List<DisplayedNavigatorLineState>,
        val distance: Float,
    ) : DisplayedNavigatorStationState
}

data class DisplayedNavigatorLineState(
    val line: Line,
    val isCurrentSelected: Boolean,
)
