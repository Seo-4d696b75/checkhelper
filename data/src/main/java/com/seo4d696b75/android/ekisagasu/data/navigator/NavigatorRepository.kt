package com.seo4d696b75.android.ekisagasu.data.navigator

import android.location.Location
import com.seo4d696b75.android.ekisagasu.data.station.Line
import com.seo4d696b75.android.ekisagasu.data.station.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 現在位置・乗車中路線から次の駅を予測する
 *
 * @author Seo-4d696b75
 * @version 2021/03/05.
 */
interface NavigatorRepository {
    val running: StateFlow<Boolean>
    val predictions: Flow<PredictionResult?>
    val line: Line?

    fun start(line: Line)

    fun stop()

    suspend fun updateLocation(
        location: Location,
        station: Station,
    )
}
