package com.seo4d696b75.android.ekisagasu.domain.error

sealed interface ErrorState {
    data object Empty : ErrorState
    data class Queued(
        val error: Throwable,
        val consumed: Boolean = false,
    ) : ErrorState

    val errorToBeShown: Throwable?
        get() = when (this) {
            Empty -> null
            is Queued -> if (consumed) null else error
        }
}
