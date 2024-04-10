package com.seo4d696b75.android.ekisagasu.data.message

import com.seo4d696b75.android.ekisagasu.domain.coroutine.ExternalScope
import com.seo4d696b75.android.ekisagasu.domain.message.AppMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class AppStateRepositoryImpl @Inject constructor(
    @ExternalScope private val scope: CoroutineScope,
) : AppStateRepository {
    override var isServiceRunning = false
    override var hasPermissionChecked = false
    override var hasDataVersionChecked = false

    private val _message = MutableSharedFlow<AppMessage>()
    private val _fixTimer = MutableStateFlow(false)
    private val _nightMode = MutableStateFlow(false)

    override val fixTimer = _fixTimer
    override val nightMode = _nightMode
    override val message = _message

    override fun setTimerFixed(fixed: Boolean) {
        _fixTimer.update { fixed }
    }

    override fun setNightMode(enabled: Boolean) {
        _nightMode.update { enabled }
    }

    override fun emitMessage(message: AppMessage) {
        scope.launch { _message.emit(message) }
    }
}
