package com.seo4d696b75.android.ekisagasu.domain.message

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface AppStateRepository {
    var isServiceRunning: Boolean
    var hasPermissionChecked: Boolean
    var hasDataVersionChecked: Boolean

    val message: SharedFlow<AppMessage>

    fun emitMessage(message: AppMessage)

    val nightMode: StateFlow<Boolean>

    fun setNightMode(enabled: Boolean)
}
