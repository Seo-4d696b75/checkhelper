package com.seo4d696b75.android.ekisagasu.domain.location

sealed interface LocationState {
    data object Idle : LocationState

    data class Running(
        val location: Location?,
        val interval: Int,
    ) : LocationState
}
