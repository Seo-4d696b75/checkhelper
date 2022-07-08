package jp.seo.station.ekisagasu.repository.impl

import jp.seo.station.ekisagasu.repository.AppStateRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class AppStateRepositoryImpl : AppStateRepository {

    override var isServiceRunning = false

    private val _startTimerEvent = MutableSharedFlow<Unit>()
    private val _fixTimer = MutableStateFlow<Boolean>(false)
    private val _nightMode = MutableStateFlow<Boolean>(false)
    private val _finishApp = MutableSharedFlow<Unit>()


    override val startTimerEvent = _startTimerEvent
    override val fixTimer = _fixTimer
    override val nightMode = _nightMode
    override val finishAppEvent = _finishApp

    override suspend fun startTimer() {
        _startTimerEvent.emit(Unit)
    }

    override suspend fun setTimerFixed(fixed: Boolean) {
        _fixTimer.emit(fixed)
    }

    override suspend fun setNightMode(enabled: Boolean) {
        _nightMode.emit(enabled)
    }

    override suspend fun finishApp() {
        _finishApp.emit(Unit)
    }
}