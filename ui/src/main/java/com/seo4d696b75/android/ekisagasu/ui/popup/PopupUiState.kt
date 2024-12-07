package com.seo4d696b75.android.ekisagasu.ui.popup

import androidx.compose.runtime.Immutable
import com.seo4d696b75.android.ekisagasu.domain.search.NearStation
import com.seo4d696b75.android.ekisagasu.ui.popup.component.StationDetectedTime

data class PopupUiState(
    val visible: Boolean,
    val isExpanded: Boolean,
    val current: PopupStationState,
) {
    companion object {
        val Initial = PopupUiState(
            visible = false,
            isExpanded = true,
            current = PopupStationState.None,
        )
    }
}

@Immutable
sealed interface PopupStationState {
    data object None : PopupStationState

    data class Result(
        val nearest: NearStation,
        val time: StationDetectedTime,
        val showPrefecture: Boolean,
    ) : PopupStationState
}
