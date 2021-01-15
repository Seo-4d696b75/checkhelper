package jp.seo.station.ekisagasu.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.core.StationService
import java.util.*

/**
 * @author Seo-4d696b75
 * @version 2021/01/14.
 */
class StationViewModel(
    private val service: StationService
) : ViewModel() {

    companion object {
        fun getFactory(service: StationService) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return StationViewModel(service) as T
            }

        }
    }

    val targetStationCode = MutableLiveData<Int?>(null)

    val station: LiveData<Station?> = targetStationCode.switchMap {
        it?.let { code ->
            service.stationRepository.getStation(code)
        } ?: MutableLiveData(null)
    }

    val lines: LiveData<List<Line>> = station.switchMap { s ->
        liveData {
            emit(
                s?.let { station ->
                    service.stationRepository.getLines(station.lines)
                } ?: Collections.emptyList<Line>()
            )
        }
    }

}
