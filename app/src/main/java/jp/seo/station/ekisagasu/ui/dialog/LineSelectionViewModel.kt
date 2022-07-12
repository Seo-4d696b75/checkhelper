package jp.seo.station.ekisagasu.ui.dialog

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.NavigationRepository
import jp.seo.station.ekisagasu.core.StationRepository
import jp.seo.station.ekisagasu.repository.LocationRepository
import javax.inject.Inject

@HiltViewModel
class LineSelectionViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val stationRepository: StationRepository,
    private val navigationRepository: NavigationRepository,
) : ViewModel() {

    private var _type = LineDialogType.SELECT_CURRENT
    private var _message = ""
    val message: String
        get() = _message

    fun setUiState(context: Context, type: LineDialogType) {
        _type = type
        _message = when (type) {
            LineDialogType.SELECT_CURRENT -> context.getString(R.string.dialog_message_select_line)
            LineDialogType.SELECT_NAVIGATION -> context.getString(R.string.dialog_message_select_navigation)
        }
    }

    val currentLine: Line?
        get() = stationRepository.selectedLine.value

    val isNavigationRunning: Boolean
        get() = navigationRepository.running.value ?: false

    val lines: List<Line>
        get() = stationRepository.nearestStations.value?.let { stations ->
            val set = mutableSetOf<Line>()
            stations.forEach { s ->
                set.addAll(s.lines)
            }
            set.toList()
        } ?: emptyList()

    fun onLineSelected(line: Line) {
        when (_type) {
            LineDialogType.SELECT_CURRENT -> selectCurrentLine(line)
            LineDialogType.SELECT_NAVIGATION -> {
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
            stationRepository.selectLine(line)
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