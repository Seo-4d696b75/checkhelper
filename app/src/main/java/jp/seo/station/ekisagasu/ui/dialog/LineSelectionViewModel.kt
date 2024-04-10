package jp.seo.station.ekisagasu.ui.dialog

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val args: LineDialogArgs by lazy {
        LineDialogArgs.fromSavedStateHandle(savedStateHandle)
    }

    @get:StringRes
    val message: Int
        get() =
            when (args.type) {
                LineDialogType.Current -> R.string.dialog_message_select_line
                LineDialogType.Navigation -> R.string.dialog_message_select_navigation
            }

    val currentLine: Line?
        get() = searchRepository.selectedLine.value

    val isNavigationRunning: Boolean
        get() = navigatorRepository.running.value

    val lines: List<Line>
        get() =
            searchRepository.nearestStations.value.let { stations ->
                val set = mutableSetOf<Line>()
                stations.forEach { s ->
                    set.addAll(s.lines)
                }
                set.toList()
            }

    fun onLineSelected(line: Line) =
        viewModelScope.launch {
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
            navigatorRepository.stop()
        } else {
            navigatorRepository.start(line)
        }
    }
}
