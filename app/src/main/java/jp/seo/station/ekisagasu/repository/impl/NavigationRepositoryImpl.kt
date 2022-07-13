package jp.seo.station.ekisagasu.repository.impl

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.model.Station
import jp.seo.station.ekisagasu.core.StationDao
import jp.seo.station.ekisagasu.position.PositionNavigator
import jp.seo.station.ekisagasu.position.PredictionResult
import jp.seo.station.ekisagasu.repository.NavigationRepository
import jp.seo.station.ekisagasu.search.NearestSearch
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class NavigationRepositoryImpl @Inject constructor(
    private val search: NearestSearch,
    private val dao: StationDao,
) : NavigationRepository {
    private val _running = MutableStateFlow(false)
    private var navigator: PositionNavigator? = null
    private val _navigator = MutableStateFlow<PositionNavigator?>(null)

    override val running: StateFlow<Boolean> = _running
    override val predictions = _navigator.flatMapLatest { n ->
        n?.results ?: flowOf(null)
    }

    override val line: Line?
        get() = navigator?.line

    override fun start(line: Line) {
        navigator?.release()
        navigator = PositionNavigator(search, dao, line)
        _navigator.value = navigator
        _running.value = true
    }

    override fun stop() {
        navigator?.release()
        navigator = null
        _navigator.value = null
        _running.value = false
    }

    override suspend fun updateLocation(location: Location, station: Station) {
        navigator?.onLocationUpdate(location, station)
    }
}