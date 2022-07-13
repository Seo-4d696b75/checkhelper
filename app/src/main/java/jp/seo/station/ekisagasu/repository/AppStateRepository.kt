package jp.seo.station.ekisagasu.repository

import jp.seo.station.ekisagasu.model.AppMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface AppStateRepository {
    var isServiceRunning: Boolean

    val message: SharedFlow<AppMessage>
    suspend fun emitMessage(message: AppMessage)

    val fixTimer: StateFlow<Boolean>
    suspend fun setTimerFixed(fixed: Boolean)
    val nightMode: StateFlow<Boolean>
    suspend fun setNightMode(enabled: Boolean)

}