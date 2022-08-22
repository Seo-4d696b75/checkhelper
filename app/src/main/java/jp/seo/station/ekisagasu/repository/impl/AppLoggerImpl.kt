package jp.seo.station.ekisagasu.repository.impl

import com.google.android.gms.common.api.ResolvableApiException
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.AppLogger
import jp.seo.station.ekisagasu.repository.AppStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class AppLoggerImpl @Inject constructor(
    private val repository: AppStateRepository
) : AppLogger {
    override fun CoroutineScope.log(message: String) {
        launch { repository.emitMessage(AppMessage.Log(message)) }
    }

    override fun CoroutineScope.error(message: String, cause: Throwable?) {
        launch { repository.emitMessage(AppMessage.Error(message, cause)) }
    }

    override fun CoroutineScope.requestExceptionResolved(
        message: String,
        e: ResolvableApiException
    ) {
        launch { repository.emitMessage(AppMessage.ResolvableException(message, e)) }
    }
}
