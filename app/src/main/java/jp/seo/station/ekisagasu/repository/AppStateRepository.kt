package jp.seo.station.ekisagasu.repository

import kotlinx.coroutines.flow.Flow

interface AppStateRepository {
    var isServiceRunning: Boolean
    val startTimerEvent: Flow<Unit>
    suspend fun startTimer()
    val fixTimer: Flow<Boolean>
    suspend fun setTimerFixed(fixed: Boolean)
    val nightMode: Flow<Boolean>
    suspend fun setNightMode(enabled: Boolean)
    val finishAppEvent: Flow<Unit>
    suspend fun finishApp()
}