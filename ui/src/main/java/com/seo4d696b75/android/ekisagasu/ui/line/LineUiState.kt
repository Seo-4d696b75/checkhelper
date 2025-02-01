package com.seo4d696b75.android.ekisagasu.ui.line

import androidx.compose.runtime.Immutable
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line

@Immutable
sealed interface LineUiState {
    data object Initializing : LineUiState

    data class Loaded(
        val line: Line,
        val stations: List<StationRegistrationUiState>,
    ) : LineUiState
}
