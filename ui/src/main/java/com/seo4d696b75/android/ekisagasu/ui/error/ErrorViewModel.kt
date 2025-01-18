package com.seo4d696b75.android.ekisagasu.ui.error

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.error.ErrorHandler
import com.seo4d696b75.android.ekisagasu.domain.error.ErrorHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class ErrorViewModel @Inject constructor(
    private val holder: ErrorHolder,
    handler: ErrorHandler,
) : ViewModel(),
    ErrorHandler by handler {
    val state = holder
        .state
        .map {
            it.errorToBeShown
        }
        .stateInCatching(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            null,
        )

    fun dismiss() {
        holder.consume()
    }
}
