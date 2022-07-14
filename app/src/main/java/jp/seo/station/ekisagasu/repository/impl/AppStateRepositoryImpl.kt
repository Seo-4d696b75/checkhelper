package jp.seo.station.ekisagasu.repository.impl

import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.repository.LogRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class AppStateRepositoryImpl @Inject constructor(
    private val logRepository: LogRepository,
) : AppStateRepository {

    override var isServiceRunning = false
    override var hasPermissionChecked = false
    override var hasDataVersionChecked = false

    private val _message = MutableSharedFlow<AppMessage>()
    private val _fixTimer = MutableStateFlow<Boolean>(false)
    private val _nightMode = MutableStateFlow<Boolean>(false)

    override val fixTimer = _fixTimer
    override val nightMode = _nightMode
    override val message = _message

    override suspend fun setTimerFixed(fixed: Boolean) {
        _fixTimer.emit(fixed)
    }

    override suspend fun setNightMode(enabled: Boolean) {
        _nightMode.emit(enabled)
    }

    override suspend fun emitMessage(message: AppMessage) {
        _message.emit(message)
        logRepository.saveMessage(message)
    }
}