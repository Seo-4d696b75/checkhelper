package com.seo4d696b75.android.ekisagasu.ui.home

import androidx.compose.runtime.Immutable
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.search.NearStation

@Immutable
sealed interface HomeUiState {

    data object Invisible : HomeUiState

    @Immutable
    sealed interface Visible : HomeUiState {
        val selectLineButton: HomeActionButtonUiState
        val lineNavigatorButton: HomeActionButtonUiState
        val timerButton: HomeActionButtonUiState.Enabled
        val mapButton: HomeActionButtonUiState.Enabled
    }

    data class Idle(
        override val timerButton: HomeActionButtonUiState.Enabled,
        override val mapButton: HomeActionButtonUiState.Enabled,
    ) : Visible {
        override val selectLineButton = HomeActionButtonUiState.Disabled
        override val lineNavigatorButton = HomeActionButtonUiState.Disabled
    }

    @Immutable
    sealed interface Running : Visible

    data class Initializing(
        override val timerButton: HomeActionButtonUiState.Enabled,
        override val mapButton: HomeActionButtonUiState.Enabled,
    ) : Running {
        override val selectLineButton = HomeActionButtonUiState.Disabled
        override val lineNavigatorButton = HomeActionButtonUiState.Disabled
    }

    data class Result(
        val station: NearStation,
        val selectedLine: Line?,
        override val selectLineButton: HomeActionButtonUiState.Enabled,
        override val lineNavigatorButton: HomeActionButtonUiState.Enabled,
        override val timerButton: HomeActionButtonUiState.Enabled,
        override val mapButton: HomeActionButtonUiState.Enabled,
    ) : Running
}

@Immutable
sealed interface HomeActionButtonUiState {
    data object Disabled : HomeActionButtonUiState

    data class Enabled(
        val onClick: () -> Unit,
    ) : HomeActionButtonUiState
}
