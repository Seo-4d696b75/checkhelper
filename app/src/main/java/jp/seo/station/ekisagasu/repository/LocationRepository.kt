package jp.seo.station.ekisagasu.repository

import android.location.Location
import kotlinx.coroutines.flow.Flow

/**
 * 現在位置を取得する機能を抽象化
 */
interface LocationRepository {
    val currentLocation: Flow<Location>
    val isRunning: Flow<Boolean>
    fun startWatchCurrentLocation(interval: Int)
    fun stopWatchCurrentLocation(): Boolean
}