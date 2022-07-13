package jp.seo.station.ekisagasu.ui.top

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.model.StationRegister
import jp.seo.station.ekisagasu.repository.DataRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LineViewModel @Inject constructor(
    private val dataRepository: DataRepository,
) : ViewModel() {

    private var _line: Line? = null
    private var _stations: List<StationRegister> = emptyList()

    fun setUiState(line: Line) = viewModelScope.launch {
        _line = line
        val indices = line.stationList.map { it.code }
        val list = dataRepository.getStations(indices)
        _stations = line.stationList.map { r ->
            val s = list.find { it.code == r.code } ?: throw NoSuchElementException()
            StationRegister(r.code, s, r.getNumberingString())
        }
    }

    val line: Line?
        get() = _line

    val stations: List<StationRegister>
        get() = _stations

    private val _event = MutableSharedFlow<LineFragmentEvent>()
    val event: SharedFlow<LineFragmentEvent> = _event

    fun close() = viewModelScope.launch {
        _event.emit(LineFragmentEvent.CloseDetail)
    }

    fun showMap() = viewModelScope.launch {
        _line?.let {
            _event.emit(LineFragmentEvent.ShowMap(it))
        }
    }
}

sealed interface LineFragmentEvent {
    object CloseDetail : LineFragmentEvent
    data class ShowMap(val line: Line) : LineFragmentEvent
}
