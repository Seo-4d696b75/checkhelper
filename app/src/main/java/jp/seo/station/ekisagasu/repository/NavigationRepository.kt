package jp.seo.station.ekisagasu.repository

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.model.Station
import jp.seo.station.ekisagasu.core.StationDao
import jp.seo.station.ekisagasu.position.PositionNavigator
import jp.seo.station.ekisagasu.position.PredictionResult
import jp.seo.station.ekisagasu.search.KdTree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 現在位置・乗車中路線から次の駅を予測する
 *
 * @author Seo-4d696b75
 * @version 2021/03/05.
 */
interface NavigationRepository {
    val running: StateFlow<Boolean>
    val predictions: Flow<PredictionResult?>
    val line: Line?
    fun start(line: Line)
    fun stop()
    suspend fun updateLocation(location: Location, station: Station)
}
