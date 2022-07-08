package jp.seo.station.ekisagasu.repository

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface AppStateRepository {
    var isServiceRunning: Boolean
    val startTimerEvent: SharedFlow<Unit>
    suspend fun startTimer()
    val fixTimer: SharedFlow<Boolean>
    suspend fun setTimerFixed(fixed: Boolean)
    val nightMode: StateFlow<Boolean>
    suspend fun setNightMode(enabled: Boolean)
    val finishAppEvent: SharedFlow<Unit>
    suspend fun finishApp()
}