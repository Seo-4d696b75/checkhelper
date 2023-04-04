package jp.seo.station.ekisagasu.repository.impl

import android.location.Location
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.model.Station
import jp.seo.station.ekisagasu.polyline.PolylineNavigator
import jp.seo.station.ekisagasu.repository.NavigationRepository
import jp.seo.station.ekisagasu.search.NearestSearch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class NavigationRepositoryImpl @Inject constructor(
    private val search: NearestSearch,
) : NavigationRepository {
    private val _running = MutableStateFlow(false)
    private var navigator: PolylineNavigator? = null
    private val _navigator = MutableStateFlow<PolylineNavigator?>(null)

    override val running: StateFlow<Boolean> = _running

    @OptIn(ExperimentalCoroutinesApi::class)
    override val predictions = _navigator.flatMapLatest { n ->
        n?.results ?: flowOf(null)
    }

    override val line: Line?
        get() = navigator?.line

    override fun start(line: Line) {
        navigator?.release()
        navigator = PolylineNavigator(search, line)
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
