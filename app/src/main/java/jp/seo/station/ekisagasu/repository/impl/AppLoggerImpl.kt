package jp.seo.station.ekisagasu.repository.impl

import com.google.android.gms.common.api.ResolvableApiException
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

class AppLoggerImpl @Inject constructor() : AppLogger {
    private val _message = MutableSharedFlow<AppMessage>(replay = 0)

    override val message: Flow<AppMessage> = _message

    override suspend fun log(message: String) {
        _message.emit(AppMessage.AppLog(message))
    }

    override suspend fun error(message: String, displayedMessage: String, cause: Throwable?) {
        _message.emit(AppMessage.AppError(message, displayedMessage, cause))
    }

    override suspend fun requestExceptionResolved(e: ResolvableApiException) {
        _message.emit(AppMessage.AppResolvableException(e))
    }
}