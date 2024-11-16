package com.seo4d696b75.android.ekisagasu.domain.location

sealed interface LocationState {
    data object Idle : LocationState

    sealed interface Running : LocationState {
        val interval: Int
    }

    data class Initializing(
        override val interval: Int,
    ) : Running

    data class Result(
        val location: Location,
        override val interval: Int,
    ) : Running
}
