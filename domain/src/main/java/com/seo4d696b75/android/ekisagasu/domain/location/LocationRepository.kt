package com.seo4d696b75.android.ekisagasu.domain.location

import kotlinx.coroutines.flow.Flow

/**
 * 現在位置を取得する機能を抽象化
 */
interface LocationRepository {
    val currentLocation: Flow<LocationState>

    suspend fun startWatchCurrentLocation()

    suspend fun stopWatchCurrentLocation(): Boolean
}
