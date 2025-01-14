package com.seo4d696b75.android.ekisagasu.ui.error

import androidx.compose.runtime.Composable
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

    fun enqueue(error: Throwable, message: ErrorUiMessage) {
        state.update { ErrorState.Queued(error, message) }
    }

    fun consume() {
        state.update {
            if (it is ErrorState.Queued && !it.consumed) {
                it.copy(consumed = true)
            } else {
                it
            }
        }
    }
}

@Immutable
sealed interface ErrorState {
    data object Empty : ErrorState
    data class Queued(
        val error: Throwable,
        val message: ErrorUiMessage,
        val consumed: Boolean = false,
    ) : ErrorState
}

data class ErrorUiMessage(
    val title: (@Composable () -> String),
    val description: (@Composable () -> String),
    val onClosed: (() -> Unit)? = null,
)
