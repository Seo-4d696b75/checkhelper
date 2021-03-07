package jp.seo.station.ekisagasu.core

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.position.PositionNavigator
import jp.seo.station.ekisagasu.position.PredictionResult
import jp.seo.station.ekisagasu.search.KdTree

/**
 * @author Seo-4d696b75
 * @version 2021/03/05.
 */
class NavigationRepository(
    private val tree: KdTree,
    private val dao: StationDao
) {

    private val _running = MutableLiveData(false)
    private var navigator: PositionNavigator? = null
    private val _navigator = MutableLiveData<PositionNavigator?>(null)

    val running: LiveData<Boolean> = _running
    val predictions = _navigator.switchMap { n ->
        n?.results ?: MutableLiveData<PredictionResult?>(null)
    }

    fun start(line: Line) {
        navigator?.release()
        navigator = PositionNavigator(tree, dao, line)
        _navigator.value = navigator
    }

    fun stop() {
        navigator?.release()
        navigator = null
        _navigator.value = null
    }

    suspend fun updateLocation(location: Location, station: Station) {
        navigator?.onLocationUpdate(location, station)
    }

}
