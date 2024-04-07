package jp.seo.station.ekisagasu.ui.top

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.model.Station
import jp.seo.station.ekisagasu.repository.DataRepository
import jp.seo.station.ekisagasu.repository.PrefectureRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StationViewModel
    @Inject
    constructor(
        private val prefectureRepository: PrefectureRepository,
        private val dataRepository: DataRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val arg by lazy {
            StationFragmentArgs.fromSavedStateHandle(savedStateHandle)
        }

        val station =
            flow {
                val station = dataRepository.getStation(arg.stationCode)
                emit(station)
            }.stateIn(viewModelScope, SharingStarted.Lazily, null)

        val lines =
            station.map {
                if (it == null) {
                    emptyList()
                } else {
                    dataRepository.getLines(it.lines)
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        val stationPrefecture =
            station.map {
                if (it == null) {
                    ""
                } else {
                    prefectureRepository.getName(it.prefecture)
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, "")

        private val _event = MutableSharedFlow<StationFragmentEvent>()
        val event: SharedFlow<StationFragmentEvent> = _event

        fun showMap() =
            viewModelScope.launch {
                station.value?.let {
                    _event.emit(StationFragmentEvent.ShowMap(it))
                }
            }

        fun close() =
            viewModelScope.launch {
                _event.emit(StationFragmentEvent.CloseDetail)
            }
    }

sealed interface StationFragmentEvent {
    object CloseDetail : StationFragmentEvent

    data class ShowMap(val station: Station) : StationFragmentEvent
}
