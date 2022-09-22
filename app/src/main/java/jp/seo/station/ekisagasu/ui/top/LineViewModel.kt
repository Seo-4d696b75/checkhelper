package jp.seo.station.ekisagasu.ui.top

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.model.StationRegister
import jp.seo.station.ekisagasu.repository.DataRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LineViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val arg by lazy {
        LineFragmentArgs.fromSavedStateHandle(savedStateHandle)
    }

    val line = flow {
        val line = dataRepository.getLine(arg.lineCode)
        emit(line)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val stations = line.map {
        if (it == null) emptyList() else {
            val indices = it.stationList.map { it.code }
            val list = dataRepository.getStations(indices)
            it.stationList.map { r ->
                val s = list.find { it.code == r.code } ?: throw NoSuchElementException()
                StationRegister(r.code, s, r.getNumberingString())
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _event = MutableSharedFlow<LineFragmentEvent>()
    val event: SharedFlow<LineFragmentEvent> = _event

    fun close() = viewModelScope.launch {
        _event.emit(LineFragmentEvent.CloseDetail)
    }

    fun showMap() = viewModelScope.launch {
        line.value?.let {
            _event.emit(LineFragmentEvent.ShowMap(it))
        }
    }
}

sealed interface LineFragmentEvent {
    object CloseDetail : LineFragmentEvent
    data class ShowMap(val line: Line) : LineFragmentEvent
}
