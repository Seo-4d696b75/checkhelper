package com.seo4d696b75.android.ekisagasu.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.error.ErrorHandler
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
import com.seo4d696b75.android.ekisagasu.domain.log.LogRepository
import com.seo4d696b75.android.ekisagasu.domain.log.filter
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.event.navigationEventHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LogComposeViewModel @Inject constructor(
    private val logRepository: LogRepository,
    handler: ErrorHandler,
) : ViewModel(),
    ErrorHandler by handler,
    NavigationEventHolder<LogComposeViewModel.Nav> by navigationEventHolder() {

    private val filter = MutableStateFlow(AppLogType.Filter.All)

    val uiState = combine(
        filter,
        logRepository.target,
        logRepository.logs,
    ) { filter, target, logs ->
        LogUiState.Loaded(
            filter = filter,
            target = target,
            logs = logs.filter(filter).toPersistentList(),
        )
    }.stateInCatching(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        LogUiState.Initializing,
    )

    fun onFilterChanged(filter: AppLogType.Filter) {
        this.filter.update { filter }
    }

    fun onSelectTargetClicked() {
        navigate(Nav.SelectLogTarget)
    }

    sealed interface Nav : NavigationEvent {
        data object SelectLogTarget : Nav
    }
}
