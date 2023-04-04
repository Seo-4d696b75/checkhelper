package jp.seo.station.ekisagasu.ui.dialog

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.repository.LocationRepository
import jp.seo.station.ekisagasu.repository.NavigationRepository
import jp.seo.station.ekisagasu.repository.SearchRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LineSelectionViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val searchRepository: SearchRepository,
    private val navigationRepository: NavigationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    sealed interface Event {
        data class Error(@StringRes val message: Int) : Event
    }

    private val _event = MutableSharedFlow<Event>()
    val event = _event.asSharedFlow()

    private val args: LineDialogArgs by lazy {
        LineDialogArgs.fromSavedStateHandle(savedStateHandle)
    }

    @get:StringRes
    val message: Int
        get() = when (args.type) {
            LineDialogType.Current -> R.string.dialog_message_select_line
            LineDialogType.Navigation -> R.string.dialog_message_select_navigation
        }

    val currentLine: Line?
        get() = searchRepository.selectedLine.value

    val isNavigationRunning: Boolean
        get() = navigationRepository.running.value

    val lines: List<Line>
        get() = searchRepository.nearestStations.value.let { stations ->
            val set = mutableSetOf<Line>()
            stations.forEach { s ->
                set.addAll(s.lines)
            }
            set.toList()
        }

    fun onLineSelected(line: Line) = viewModelScope.launch {
        when (args.type) {
            LineDialogType.Current -> selectCurrentLine(line)
            LineDialogType.Navigation -> {
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
        if (line == null) {
            navigationRepository.stop()
        } else {
            navigationRepository.start(line)
        }
    }
}
