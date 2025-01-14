package com.seo4d696b75.android.ekisagasu.ui.error

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ErrorViewModel @Inject constructor(
    private val holder: ErrorStateHolder,
) : ViewModel() {
    val state = holder
        .errorState
        .map {
            if (it is ErrorState.Queued && !it.consumed) {
                it
            } else {
                null
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            null,
        )

    fun dismiss() {
        holder.consume()
    }
}
