package jp.seo.station.ekisagasu.repository

import com.google.android.gms.common.api.ResolvableApiException
import jp.seo.station.ekisagasu.model.AppMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface AppLogger {
    suspend fun log(message: String)
    suspend fun error(message: String, displayedMessage: String, cause: Throwable? = null)
    suspend fun requestExceptionResolved(e: ResolvableApiException)
    val message: Flow<AppMessage>
}

interface LogEmitter {
    fun CoroutineScope.log(message: String)
    fun CoroutineScope.error(message: String, displayedMessage: String)
    fun CoroutineScope.requestExceptionResolved(e: ResolvableApiException)
}

@Singleton
class LogEmitterImpl @Inject constructor(
    private val logger: AppLogger
) : LogEmitter {
    override fun CoroutineScope.log(message: String) {
        launch { logger.log(message) }
    }

    override fun CoroutineScope.error(message: String, displayedMessage: String) {
        launch { logger.error(message, displayedMessage) }
    }

    override fun CoroutineScope.requestExceptionResolved(e: ResolvableApiException) {
        launch { logger.requestExceptionResolved(e) }
    }

}