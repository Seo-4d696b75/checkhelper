package com.seo4d696b75.android.ekisagasu.data.log

import android.content.Context
import com.seo4d696b75.android.ekisagasu.data.R
import com.seo4d696b75.android.ekisagasu.data.database.user.AppLogEntity
import com.seo4d696b75.android.ekisagasu.data.database.user.AppRebootEntity
import com.seo4d696b75.android.ekisagasu.data.database.user.UserDao
import com.seo4d696b75.android.ekisagasu.domain.config.AppConfig
import com.seo4d696b75.android.ekisagasu.domain.date.TIME_PATTERN_DATETIME
import com.seo4d696b75.android.ekisagasu.domain.date.TIME_PATTERN_ISO8601_EXTEND
import com.seo4d696b75.android.ekisagasu.domain.date.format
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogTarget
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
import com.seo4d696b75.android.ekisagasu.domain.log.LogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
class LogRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: AppConfig,
    private val dao: UserDao,
) : LogRepository {

    private lateinit var reboot: AppRebootEntity
    private val _target = MutableStateFlow<AppLogTarget?>(null)

    private var _hasError = false

    override suspend fun write(
        type: AppLogType,
        message: String,
        isError: Boolean,
    ) = withContext(Dispatchers.IO) {
        val log = AppLogEntity(type, message)
        _hasError = _hasError || isError
        dao.insertLog(log)
    }

    override val history = dao.getRebootHistory().map { list ->
        list.mapIndexed { i, log ->
            // idに関して降順でソートされている
            AppLogTarget(
                id = log.id,
                range = log.id..(if (i > 0) list[i - 1].id else Long.MAX_VALUE),
                start = log.start,
                end = log.finish,
                hasError = log.error,
            )
        }
    }

    override val target = _target.filterNotNull()

    override fun setTarget(target: AppLogTarget) {
        _target.update { target }
    }

    override val logs = _target
        .filterNotNull()
        .flatMapLatest { target ->
            dao.getLogs(target.range.first, target.range.last)
                .map { list -> list.map { it.toModel() } }
        }

    override suspend fun onAppBoot() = withContext(Dispatchers.IO) {
        val mes = context.getString(
            R.string.log_message_start_app,
            Date().format(TIME_PATTERN_ISO8601_EXTEND),
        )
        val log = AppLogEntity(AppLogType.System, mes)
        dao.insertRebootLog(log)
        val current = dao.getCurrentReboot()
        _target.update {
            AppLogTarget(
                id = current.id,
                range = current.id..Long.MAX_VALUE,
                start = current.start,
                end = current.finish,
                hasError = false,
            )
        }
        reboot = current
    }

    override suspend fun onAppFinish() = withContext(Dispatchers.IO) {
        if (_hasError) {
            writeErrorLog(
                config.appName,
                context.getExternalFilesDir(null),
            )
        }
        val log = AppLogEntity(AppLogType.System, context.getString(R.string.log_message_fin_app))
        dao.insertLog(log)
        dao.writeFinish(log.timestamp, _hasError)
    }

    private suspend fun writeErrorLog(
        title: String,
        dir: File?,
    ) {
        val logs = reboot.let {
            dao.getLogsOneshot(it.id, Long.MAX_VALUE)
        }
        withContext(Dispatchers.IO) {
            try {
                val builder = StringBuilder()
                builder.append(title)
                builder.append("\n")
                val time = Date().format(TIME_PATTERN_DATETIME)
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

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface LogRepositoryModule {
    @Binds
    @Singleton
    fun bindsLogRepository(impl: LogRepositoryImpl): LogRepository
}
