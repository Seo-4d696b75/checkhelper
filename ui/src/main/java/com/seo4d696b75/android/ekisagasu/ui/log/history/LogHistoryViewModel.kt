package com.seo4d696b75.android.ekisagasu.ui.log.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.error.ErrorHandler
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogTarget
import com.seo4d696b75.android.ekisagasu.domain.log.LogRepository
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.event.navigationEventHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class LogHistoryViewModel @Inject constructor(
    private val logRepository: LogRepository,
    handler: ErrorHandler,
) : ViewModel(),
    ErrorHandler by handler,
    NavigationEventHolder<LogHistoryViewModel.Close> by navigationEventHolder() {

    val uiState = logRepository
        .history
        .map {
            it.toPersistentList()
        }
        .stateInCatching(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            persistentListOf(),
        )

    fun onTargetSelected(target: AppLogTarget) {
        logRepository.setTarget(target)
        navigate(Close)
    }

    fun onClose() {
        navigate(Close)
    }

    data object Close : NavigationEvent
}
