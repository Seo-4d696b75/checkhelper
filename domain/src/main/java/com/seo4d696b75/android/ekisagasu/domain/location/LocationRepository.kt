package com.seo4d696b75.android.ekisagasu.domain.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 現在位置を取得する機能を抽象化
 */
interface LocationRepository {
    val currentLocation: Flow<Location?>
    val isRunning: StateFlow<Boolean>

    suspend fun startWatchCurrentLocation(interval: Int)

    suspend fun stopWatchCurrentLocation(): Boolean
}
