package com.seo4d696b75.android.ekisagasu.ui.error

import androidx.compose.runtime.Immutable
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ActivityRetainedScoped
class ErrorStateHolder @Inject constructor() {

    private val state = MutableStateFlow<ErrorState>(ErrorState.Empty)
    val errorState = state.asStateFlow()

    fun enqueue(error: Throwable) {
        state.update { ErrorState.Queued(error) }
    }

    fun consume() {
        state.update {
            require(it is ErrorState.Queued)
            it.copy(consumed = true)
        }
    }
}

@Immutable
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
