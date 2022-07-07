package jp.seo.station.ekisagasu.repository.impl

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ResolvableApiException
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.core.AppLog
import jp.seo.station.ekisagasu.core.AppRebootLog
import jp.seo.station.ekisagasu.core.UserDao
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.LogRepository
import jp.seo.station.ekisagasu.model.LogTarget
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_DATETIME
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_ISO8601_EXTEND
import jp.seo.station.ekisagasu.utils.formatTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
class LogRepositoryImpl @Inject constructor(
    private val dao: UserDao,
    defaultDispatcher: CoroutineDispatcher,
) : LogRepository, CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext = defaultDispatcher + job

    private val _logFilter = MutableStateFlow(LogTarget(null, Long.MAX_VALUE))

    private var _hasError = false

    private val _message = MutableSharedFlow<AppMessage>()

    private suspend fun saveLog(type: Int, message: String) = withContext(Dispatchers.IO) {
        val log = AppLog(type, message)
        dao.insertLog(log)
    }

    override suspend fun logMessage(message: String) {
        Log.i("AppMessage.Log", message)
        _message.emit(AppMessage.Log(message))
        saveLog(AppLog.TYPE_SYSTEM, message)
    }

    override suspend fun logError(message: String, cause: Throwable?) {
        Log.e("AppMessage.Error", message)
        _message.emit(AppMessage.Error(message, cause))
        saveLog(AppLog.TYPE_SYSTEM, message)
        _hasError = true
    }

    override suspend fun requestExceptionResolved(message: String, e: ResolvableApiException) {
        Log.w("AppMessage.ResolvableException", "$message: $e")
        _message.emit(AppMessage.ResolvableException(e))
        saveLog(AppLog.TYPE_SYSTEM, message)
    }

    override suspend fun logLocation(lat: Double, lng: Double) {
        val mes = String.format("(%.6f,%.6f)", lat, lng)
        Log.i("Location", mes)
        saveLog(AppLog.TYPE_LOCATION, mes)
    }

    override suspend fun logStation(station: Station) {
        val mes = String.format("%s(%d)", station.name, station.code)
        Log.i("Station", mes)
        saveLog(AppLog.TYPE_STATION, mes)
    }

    override val message: SharedFlow<AppMessage>
        get() = _message

    override val history: StateFlow<List<AppRebootLog>>
        get() = dao.getRebootHistory().stateIn(this, SharingStarted.Eagerly, emptyList())

    override val logFilter: StateFlow<LogTarget>
        get() = _logFilter

    override suspend fun filterLogSince(since: AppRebootLog) = withContext(Dispatchers.IO) {
        val until = dao.getNextReboot(since.id)
        _logFilter.emit(LogTarget(since, since.id, until ?: Long.MAX_VALUE))
    }

    override val logs: StateFlow<List<AppLog>>
        get() = _logFilter.flatMapLatest {
            dao.getLogs(it.since, it.until)
        }.stateIn(this, SharingStarted.Eagerly, emptyList())

    override suspend fun onAppBoot(context: Context) = withContext(Dispatchers.IO) {
        val mes =
            "app started at " + formatTime(TIME_PATTERN_ISO8601_EXTEND, Date())
        val log = AppLog(AppLog.TYPE_SYSTEM, mes)
        dao.insertRebootLog(log)
        val current = dao.getCurrentReboot()
        _logFilter.emit(LogTarget(current, current.id))
    }

    override suspend fun onAppFinish(context: Context) = withContext(Dispatchers.IO) {
        if (_hasError) {
            writeErrorLog(
                context.getString(R.string.app_name),
                context.getExternalFilesDir(null)
            )
        }
        val log = AppLog(AppLog.TYPE_SYSTEM, "finish app")
        dao.insertLog(log)
        dao.writeFinish(log.timestamp, _hasError)

        job.cancel()
    }

    private suspend fun writeErrorLog(title: String, dir: File?) {
        val logs = this.logs.value
        withContext(Dispatchers.IO) {
            try {
                val builder = StringBuilder()
                builder.append(title)
                builder.append("\n")
                val time = formatTime(TIME_PATTERN_DATETIME, Date())
                builder.append(
                    String.format(
                        "crash time: %s\n",
                        time
                    )
                )
                logs.forEach { log ->
                    builder.append(log.toString())
                    builder.append("\n")
                }
                val fileName = String.format("ErrorLog_%s.txt", time)
                File(dir, fileName).writeText(builder.toString(), Charsets.UTF_8)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}