package jp.seo.station.ekisagasu.repository

import jp.seo.station.ekisagasu.model.AppMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface AppStateRepository {
    var isServiceRunning: Boolean
    var hasPermissionChecked: Boolean
    var hasDataVersionChecked: Boolean

    val message: SharedFlow<AppMessage>

    suspend fun emitMessage(message: AppMessage)

    val fixTimer: StateFlow<Boolean>

    fun setTimerFixed(fixed: Boolean)

    val nightMode: StateFlow<Boolean>

    fun setNightMode(enabled: Boolean)
}
