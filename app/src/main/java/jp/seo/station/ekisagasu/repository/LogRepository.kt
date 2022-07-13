package jp.seo.station.ekisagasu.repository

import android.content.Context
import com.google.android.gms.common.api.ResolvableApiException
import jp.seo.station.ekisagasu.core.AppLog
import jp.seo.station.ekisagasu.core.AppRebootLog
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.model.LogTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

interface LogRepository {
    suspend fun saveMessage(message: AppMessage)
    val history: StateFlow<List<AppRebootLog>>
    val logFilter: StateFlow<LogTarget>
    suspend fun filterLogSince(since: AppRebootLog)
    val logs: StateFlow<List<AppLog>>
    suspend fun onAppBoot(context: Context)
    suspend fun onAppFinish(context: Context)
}

interface AppLogger {
    fun CoroutineScope.log(message: String)
    fun CoroutineScope.error(message: String, cause: Throwable? = null)
    fun CoroutineScope.requestExceptionResolved(message: String, e: ResolvableApiException)
}

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