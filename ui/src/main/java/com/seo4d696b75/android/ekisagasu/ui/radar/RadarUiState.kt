package com.seo4d696b75.android.ekisagasu.ui.radar

import androidx.compose.runtime.Immutable
import com.seo4d696b75.android.ekisagasu.domain.search.NearStation

@Immutable
sealed interface RadarUiState {

    data object None : RadarUiState

    @Immutable
    sealed interface Running : RadarUiState {
        val size: Int
    }

    data class Initializing(
        override val size: Int
    ) : Running

    data class Data(
        override val size: Int,
        val list: List<NearStation>,
    ) : Running
}
