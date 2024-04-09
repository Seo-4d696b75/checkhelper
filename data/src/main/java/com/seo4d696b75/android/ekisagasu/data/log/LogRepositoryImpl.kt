package com.seo4d696b75.android.ekisagasu.data.log

import android.content.Context
import com.seo4d696b75.android.ekisagasu.data.R
import com.seo4d696b75.android.ekisagasu.data.config.AppConfig
import com.seo4d696b75.android.ekisagasu.data.database.AppLog
import com.seo4d696b75.android.ekisagasu.data.database.AppRebootLog
import com.seo4d696b75.android.ekisagasu.data.database.UserDao
import com.seo4d696b75.android.ekisagasu.data.utils.TIME_PATTERN_DATETIME
import com.seo4d696b75.android.ekisagasu.data.utils.TIME_PATTERN_ISO8601_EXTEND
import com.seo4d696b75.android.ekisagasu.data.utils.formatTime
import dagger.hilt.android.qualifiers.ApplicationContext
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
import java.util.Date
import javax.inject.Inject

@ExperimentalCoroutinesApi
class LogRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: AppConfig,
    private val dao: UserDao,
) : LogRepository {
    private lateinit var runningLogTarget: LogTarget
    private val _logFilter = MutableStateFlow(LogTarget(null, Long.MAX_VALUE))

    private var _hasError = false

    override suspend fun write(
        type: AppLogType,
        message: String,
        isError: Boolean,
    ) = withContext(Dispatchers.IO) {
        val log = AppLog(type, message)
        _hasError = _hasError || isError
        dao.insertLog(log)
    }

    override val history = dao.getRebootHistory()

    override val filter: StateFlow<LogTarget>
        get() = _logFilter

    override suspend fun filterLogSince(since: AppRebootLog) =
        withContext(Dispatchers.IO) {
            val until = dao.getNextReboot(since.id)
            _logFilter.emit(LogTarget(since, since.id, until ?: Long.MAX_VALUE))
        }

    override val logs =
        _logFilter.flatMapLatest {
            dao.getLogs(it.since, it.until)
        }

    override suspend fun onAppBoot() = withContext(Dispatchers.IO) {
        val mes = context.getString(R.string.log_message_start_app, formatTime(TIME_PATTERN_ISO8601_EXTEND, Date()))
        val log = AppLog(AppLogType.System, mes)
        dao.insertRebootLog(log)
        val current = dao.getCurrentReboot()
        val target = LogTarget(current, current.id)
        _logFilter.update { target }
        runningLogTarget = target
    }

    override suspend fun onAppFinish() = withContext(Dispatchers.IO) {
        if (_hasError) {
            writeErrorLog(
                config.appName,
                context.getExternalFilesDir(null),
            )
        }
        val log = AppLog(AppLogType.System, context.getString(R.string.log_message_fin_app))
        dao.insertLog(log)
        dao.writeFinish(log.timestamp, _hasError)
    }

    private suspend fun writeErrorLog(
        title: String,
        dir: File?,
    ) {
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
                        time,
                    ),
                )
                logs.forEach { log ->
                    builder.append(log.toString())
                    builder.append("\n")
                }
                val fileName = String.format("ErrorLog_%s.txt", time)
                File(dir, fileName).writeText(builder.toString(), Charsets.UTF_8)
            } catch (e: IOException) {
                Timber.w(e)
            }
        }
    }
}
