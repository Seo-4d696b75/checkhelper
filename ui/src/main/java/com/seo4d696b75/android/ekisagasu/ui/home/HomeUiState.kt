package com.seo4d696b75.android.ekisagasu.ui.home

import androidx.compose.runtime.Immutable
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.search.NearStation

@Immutable
sealed interface HomeUiState {
    data object Idle : HomeUiState

    @Immutable
    sealed interface Running : HomeUiState

    data object Initializing : Running
    data class Result(
        val station: NearStation,
        val selectedLine: Line?,
    ) : Running
}
