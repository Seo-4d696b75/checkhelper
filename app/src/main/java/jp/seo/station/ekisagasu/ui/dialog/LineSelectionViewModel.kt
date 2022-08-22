package jp.seo.station.ekisagasu.ui.dialog

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.repository.LocationRepository
import jp.seo.station.ekisagasu.repository.NavigationRepository
import jp.seo.station.ekisagasu.repository.SearchRepository
import javax.inject.Inject

@HiltViewModel
class LineSelectionViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val searchRepository: SearchRepository,
    private val navigationRepository: NavigationRepository,
) : ViewModel() {

    private var _type = LineDialogType.Current
    private var _message = ""
    val message: String
        get() = _message

    fun setUiState(context: Context, type: LineDialogType) {
        _type = type
        _message = when (type) {
            LineDialogType.Current -> context.getString(R.string.dialog_message_select_line)
            LineDialogType.Navigation -> context.getString(R.string.dialog_message_select_navigation)
        }
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

    fun onLineSelected(line: Line) {
        when (_type) {
            LineDialogType.Current -> selectCurrentLine(line)
            LineDialogType.Navigation -> {
                if (line.polyline == null) {
                    // TODO
                    // viewModel.requestToast.call(getString(R.string.navigation_unsupported))
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
