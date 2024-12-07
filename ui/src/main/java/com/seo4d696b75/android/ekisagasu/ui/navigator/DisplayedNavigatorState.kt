package com.seo4d696b75.android.ekisagasu.ui.navigator

import androidx.compose.runtime.Immutable
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station

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

    data class Current(
        override val station: Station,
    ) : DisplayedNavigatorStationState

    data class Prediction(
        override val station: Station,
        val distance: Float,
    ) : DisplayedNavigatorStationState
}
