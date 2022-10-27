package jp.seo.station.ekisagasu.repository.impl

import android.content.Context
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.database.AppLog
import jp.seo.station.ekisagasu.database.AppRebootLog
import jp.seo.station.ekisagasu.database.UserDao
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.model.LogTarget
import jp.seo.station.ekisagasu.repository.LogRepository
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_DATETIME
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_ISO8601_EXTEND
import jp.seo.station.ekisagasu.utils.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date
import javax.inject.Inject

@ExperimentalCoroutinesApi
class LogRepositoryImpl @Inject constructor(
    private val dao: UserDao,
) : LogRepository {

    private lateinit var runningLogTarget: LogTarget
    private val _logFilter = MutableStateFlow(LogTarget(null, Long.MAX_VALUE))

    private var _hasError = false

    private suspend fun saveLog(type: Int, message: String) = withContext(Dispatchers.IO) {
        val str = if (message.length > LogRepository.MAX_MESSAGE_LENGTH) {
            message.substring(0, LogRepository.MAX_MESSAGE_LENGTH) +
                    " ...(and ${message.length - LogRepository.MAX_MESSAGE_LENGTH}letters)"
        } else message
        val log = AppLog(type, str)
        dao.insertLog(log)
    }

    override suspend fun saveMessage(message: AppMessage) {
        when (message) {
            is AppMessage.Log -> {
                saveLog(AppLog.TYPE_SYSTEM, message.message)
            }
            is AppMessage.Error -> {
                val cause = message.cause
                val str = if (cause != null) {
                    val sw = StringWriter()
                    val pw = PrintWriter(sw)
                    cause.printStackTrace(pw)
                    String.format("%s caused by;\n%s", message.message, sw.toString())
                } else message.message
                saveLog(AppLog.TYPE_SYSTEM, str)
                _hasError = true
            }
            is AppMessage.ResolvableException -> {
                Timber.tag("ResolvableException").d("${message.message}: ${message.exception}")
                saveLog(AppLog.TYPE_SYSTEM, message.message)
            }
            is AppMessage.Location -> {
                val mes = String.format("(%.6f,%.6f)", message.lat, message.lng)
                Timber.tag("Location").d(mes)
                saveLog(AppLog.TYPE_LOCATION, mes)
            }
            is AppMessage.Station -> {
                val station = message.station
                val mes = String.format("%s(%d)", station.name, station.code)
                Timber.tag("Station").d(mes)
                saveLog(AppLog.TYPE_STATION, mes)
            }
            else -> {
                Timber.tag("AppMessage").d(message.toString())
            }
        }
    }

    override val history = dao.getRebootHistory()

    override val logFilter: StateFlow<LogTarget>
        get() = _logFilter

    override suspend fun filterLogSince(since: AppRebootLog) = withContext(Dispatchers.IO) {
        val until = dao.getNextReboot(since.id)
        _logFilter.emit(LogTarget(since, since.id, until ?: Long.MAX_VALUE))
    }

    override val logs = _logFilter.flatMapLatest {
        dao.getLogs(it.since, it.until)
    }

    override suspend fun onAppBoot(context: Context) = withContext(Dispatchers.IO) {
        val mes =
            "app started at " + formatTime(TIME_PATTERN_ISO8601_EXTEND, Date())
        val log = AppLog(AppLog.TYPE_SYSTEM, mes)
        dao.insertRebootLog(log)
        val current = dao.getCurrentReboot()
        val target = LogTarget(current, current.id)
        _logFilter.update { target }
        runningLogTarget = target
    }

    override suspend fun onAppFinish(context: Context) = withContext(Dispatchers.IO) {
        if (_hasError) {
            writeErrorLog(
                context.getString(R.string.app_name),
                context.getExternalFilesDir(null)
            )
        }
        val log = AppLog(AppLog.TYPE_SYSTEM, context.getString(R.string.message_fin_app))
        dao.insertLog(log)
        dao.writeFinish(log.timestamp, _hasError)
    }

    private suspend fun writeErrorLog(title: String, dir: File?) {
        val logs = runningLogTarget.let {
            dao.getLogsOneshot(it.since, it.until)
        }
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
