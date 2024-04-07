package jp.seo.station.ekisagasu.repository

import android.location.Location
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 現在位置を取得する機能を抽象化
 */
interface LocationRepository {
    val currentLocation: SharedFlow<Location>
    val isRunning: StateFlow<Boolean>

    fun startWatchCurrentLocation(interval: Int)

    fun stopWatchCurrentLocation(): Boolean
}
