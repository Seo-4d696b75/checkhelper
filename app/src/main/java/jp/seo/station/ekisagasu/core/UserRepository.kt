package jp.seo.station.ekisagasu.core

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.model.UserSetting
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_DATETIME
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_ISO8601_EXTEND
import jp.seo.station.ekisagasu.utils.formatTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
@ExperimentalCoroutinesApi
class UserRepository(
    private val dao: UserDao,
    defaultDispatcher: CoroutineDispatcher,
) : CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext = defaultDispatcher + job

    val setting = MutableStateFlow(UserSetting())

    private val _logFilter = MutableStateFlow(LogTarget(null, Long.MAX_VALUE))
    private var _hasError = false

    val history = dao.getRebootHistory()

    val currentLogTarget: StateFlow<LogTarget?>
        get() = _logFilter

    suspend fun selectLogsSince(since: AppRebootLog) = withContext(Dispatchers.IO) {
        val until = dao.getNextReboot(since.id)
        _logFilter.emit(LogTarget(since, since.id, until ?: Long.MAX_VALUE))
    }

    val logs: StateFlow<List<AppLog>> = _logFilter.flatMapLatest {
        dao.getLogs(it.since, it.until)
    }.stateIn(this, SharingStarted.Eagerly, emptyList())

    suspend fun logMessage(message: String) {
        log(AppLog.TYPE_SYSTEM, message)
    }

    suspend fun logError(message: String) {
        log(AppLog.TYPE_SYSTEM, message)
        _hasError = true
    }

    suspend fun logLocation(lat: Double, lng: Double) {
        val mes = String.format("(%.6f,%.6f)", lat, lng)
        log(AppLog.TYPE_LOCATION, mes)
    }

    suspend fun logStation(station: String) {
        log(AppLog.TYPE_STATION, station)
    }

    suspend fun onAppReboot(context: Context) {
        val mes =
            "app started at " + formatTime(TIME_PATTERN_ISO8601_EXTEND, Date())
        val log = AppLog(AppLog.TYPE_SYSTEM, mes)
        dao.insertRebootLog(log)
        val current = dao.getCurrentReboot()
        _logFilter.emit(LogTarget(current, current.id))

        val reference = context.getSharedPreferences(
            context.getString(R.string.preference_setting_backup),
            Context.MODE_PRIVATE
        )
        setting.emit(UserSetting.load(reference))
    }

    private suspend fun log(type: Int, message: String) = withContext(Dispatchers.IO) {
        val log = AppLog(type, message)
        Log.d("AppLog", log.toString())
        dao.insertLog(log)
    }

    @MainThread
    suspend fun onAppFinish(context: Context) = withContext(Dispatchers.IO) {
        val reference = context.getSharedPreferences(
            context.getString(R.string.preference_setting_backup),
            Context.MODE_PRIVATE
        )
        setting.value.save(reference)

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

data class LogTarget(
    val target: AppRebootLog?,
    val since: Long,
    val until: Long = Long.MAX_VALUE
)
