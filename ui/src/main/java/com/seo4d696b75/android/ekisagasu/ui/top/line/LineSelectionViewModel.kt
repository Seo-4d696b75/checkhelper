package com.seo4d696b75.android.ekisagasu.ui.top.line

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.ui.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LineSelectionViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val searchRepository: StationSearchRepository,
    private val navigatorRepository: NavigatorRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    sealed interface Event {
        data class Error(@StringRes val message: Int) : Event
    }

    private val _event = MutableSharedFlow<Event>()
    val event = _event.asSharedFlow()

    private val args: LineSelectDialogArgs by lazy {
        LineSelectDialogArgs.fromSavedStateHandle(savedStateHandle)
    }

    @get:StringRes
    val message: Int
        get() =
            when (args.type) {
                LineSelectType.Current -> R.string.dialog_message_select_line
                LineSelectType.Navigation -> R.string.dialog_message_select_navigation
            }

    val currentLine: Line?
        get() = searchRepository.selectedLine.value

    val navigatorLine: Line?
        get() = navigatorRepository.currentLine

    val lines = searchRepository
        .result
        .filterNotNull()
        .map {
            val set = mutableSetOf<Line>()
            it.nears.forEach { s ->
                set.addAll(s.lines)
            }
            set.toList()
        }

    fun onLineSelected(line: Line) = viewModelScope.launch {
        when (args.type) {
            LineSelectType.Current -> selectCurrentLine(line)
            LineSelectType.Navigation -> {
                if (line.polyline == null) {
                    _event.emit(Event.Error(R.string.navigation_unsupported))
                } else {
                    selectNavigationLine(line)
                }
            }
        }
    }

    fun selectCurrentLine(line: Line?) {
        if (locationRepository.isRunning.value) {
            searchRepository.selectLine(line)
        }
    }

    fun selectNavigationLine(line: Line?) {
        selectCurrentLine(line)
        navigatorRepository.setLine(line)
    }
}
