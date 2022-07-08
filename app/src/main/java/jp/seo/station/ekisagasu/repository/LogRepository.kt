package jp.seo.station.ekisagasu.repository

import android.content.Context
import com.google.android.gms.common.api.ResolvableApiException
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.core.AppLog
import jp.seo.station.ekisagasu.core.AppRebootLog
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.model.LogTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

interface LogRepository {
    suspend fun logMessage(message: String)
    suspend fun logError(message: String, cause: Throwable? = null)
    suspend fun requestExceptionResolved(message: String, e: ResolvableApiException)
    suspend fun logLocation(lat: Double, lng: Double)
    suspend fun logStation(station: Station)
    val message: SharedFlow<AppMessage>
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
    private val repository: LogRepository
) : AppLogger {
    override fun CoroutineScope.log(message: String) {
        launch { repository.logMessage(message) }
    }

    override fun CoroutineScope.error(message: String, cause: Throwable?) {
        launch { repository.logError(message, cause) }
    }

    override fun CoroutineScope.requestExceptionResolved(message: String, e: ResolvableApiException) {
        launch { repository.requestExceptionResolved(message, e) }
    }
}