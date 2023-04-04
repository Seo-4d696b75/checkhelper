package jp.seo.station.ekisagasu.repository

import android.location.Location
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.model.Station
import jp.seo.station.ekisagasu.polyline.PredictionResult
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
