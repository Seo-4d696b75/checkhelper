package com.seo4d696b75.android.ekisagasu.ui.station

import androidx.compose.runtime.Immutable
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station

@Immutable
sealed interface StationUiState {
    data object Initializing : StationUiState

    data class Loaded(
        val station: Station,
        val lines: List<Line>,
    ) : StationUiState
}
