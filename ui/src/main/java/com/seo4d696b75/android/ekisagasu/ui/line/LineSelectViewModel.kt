package com.seo4d696b75.android.ekisagasu.ui.line

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.error.ErrorHandler
import com.seo4d696b75.android.ekisagasu.domain.error.PolylineNotSupportedException
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorState
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchState
import com.seo4d696b75.android.ekisagasu.ui.R
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.event.navigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationRoute
import com.seo4d696b75.android.ekisagasu.ui.navigation.typeMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class LineSelectViewModel @Inject constructor(
    private val searchRepository: StationSearchRepository,
    private val navigatorRepository: NavigatorRepository,
    savedStateHandle: SavedStateHandle,
    handler: ErrorHandler,
) : ViewModel(),
    NavigationEventHolder<LineSelectViewModel.Nav> by navigationEventHolder(),
    ErrorHandler by handler {

    private val args = savedStateHandle.toRoute<NavigationRoute.SelectLineDialog>(typeMap)

    private val clearEnabled = when (args.type) {
        LineSelectType.Current -> searchRepository.selectedLine.map { it != null }
        LineSelectType.Navigator -> navigatorRepository.state.map { it is NavigatorState.Running }
    }

    @get:StringRes
    private val message: Int
        get() = when (args.type) {
            LineSelectType.Current -> R.string.dialog_message_select_line
            LineSelectType.Navigator -> R.string.dialog_message_select_navigation
        }

    val uiState = combine(
        clearEnabled,
        searchRepository
            .state
            .filterIsInstance<StationSearchState.Result>()
            .map {
                mutableSetOf<Line>().apply {
                    it.nears.forEach { s -> addAll(s.station.lines) }
                }.toList()
            },
    ) { clearEnabled, lines ->
        LineSelectUiState(
            clearEnabled = clearEnabled,
            message = message,
            lines = lines.toPersistentList(),
        )
    }.stateInCatching(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        LineSelectUiState.Initial,
    )

    fun onLineSelected(line: Line) = viewModelScope.launchCatching {
        when (args.type) {
            LineSelectType.Current -> {
                searchRepository.selectLine(line)
                navigate(Nav.Close)
            }

            LineSelectType.Navigator -> if (line.polyline == null) {
                throw PolylineNotSupportedException(line)
            } else {
                navigatorRepository.start(line)
                navigate(Nav.Close)
            }
        }
    }

    fun onLineCleared() {
        when (args.type) {
            LineSelectType.Current -> {
                searchRepository.clearLine()
                navigate(Nav.Close)
            }

            LineSelectType.Navigator -> {
                navigatorRepository.stop()
                navigate(Nav.Close)
            }
        }
    }

    fun onCloseClicked() {
        navigate(Nav.Close)
    }

    sealed interface Nav : NavigationEvent {
        data object Close : Nav
    }
}
