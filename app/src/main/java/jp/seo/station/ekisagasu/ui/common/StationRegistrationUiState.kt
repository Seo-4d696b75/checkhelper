package jp.seo.station.ekisagasu.ui.common

import com.seo4d696b75.android.ekisagasu.domain.dataset.Station

data class StationRegistrationUiState(
    val code: Int,
    val station: Station,
    val numbering: String,
)
