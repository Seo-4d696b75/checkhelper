package jp.seo.station.ekisagasu.ui.top

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.core.PrefectureRepository
import jp.seo.station.ekisagasu.core.StationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StationViewModel @Inject constructor(
    private val prefectureRepository: PrefectureRepository,
    private val stationRepository: StationRepository,
) : ViewModel() {

    fun setUiState(station: Station) = viewModelScope.launch {
        _station = station
        _lines = stationRepository.getLines(station.lines)
    }

    private var _station: Station? = null
    private var _lines: List<Line> = emptyList()

    val station: Station?
        get() = _station

    val lines: List<Line>
        get() = _lines

    val stationPrefecture: String
        get() = _station?.let { prefectureRepository.getName(it.prefecture) } ?: ""

    private val _event = MutableSharedFlow<StationFragmentEvent>()
    val event: SharedFlow<StationFragmentEvent> = _event

    fun showMap() = viewModelScope.launch {
        _station?.let {
            _event.emit(StationFragmentEvent.ShowMap(it))
        }
    }

    fun close() = viewModelScope.launch {
        _event.emit(StationFragmentEvent.CloseDetail)
    }
}

sealed interface StationFragmentEvent {
    object CloseDetail : StationFragmentEvent
    data class ShowMap(val station: Station) : StationFragmentEvent
}
