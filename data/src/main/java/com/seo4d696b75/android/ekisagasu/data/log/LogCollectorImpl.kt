package com.seo4d696b75.android.ekisagasu.data.log

import android.content.Context
import com.seo4d696b75.android.ekisagasu.data.R
import com.seo4d696b75.android.ekisagasu.data.utils.ExternalScope
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject

class LogCollectorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @ExternalScope private val scope: CoroutineScope,
    private val repository: LogRepository,
) : LogCollector {
    override fun log(message: LogMessage) {
        scope.launch(Dispatchers.Default) {
            val str = message.toString(context)
            repository.write(
                type = message.type,
                message = str.trim(500),
                isError = message.isError,
            )
        }
    }
}

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface LogCollectorModule {
    @Binds
    fun bindLogCollector(impl: LogCollectorImpl): LogCollector
}

private fun LogMessage.toString(context: Context): String =
    when (this) {
        is LogMessage.Location -> String.format("(%.6f,%.6f)", lat, lng)
        is LogMessage.Station -> String.format("%s(%d)", station.name, station.code)
        is LogMessage.GPS.Start -> context.getString(R.string.log_message_gps_start, interval)
        is LogMessage.GPS.IntervalChanged -> context.getString(R.string.log_message_gps_min_interval, before, after)
        LogMessage.GPS.Stop -> context.getString(R.string.log_message_gps_end)
        LogMessage.GPS.NoPermission -> context.getString(R.string.log_message_gps_permission_not_granted)
        is LogMessage.GPS.ResolvableException -> context.getString(R.string.log_message_gps_resolvable_exception)
        is LogMessage.Data.Found -> context.getString(R.string.log_message_saved_data_found, version.version)
        is LogMessage.Data.DownloadRequired -> context.getString(
            R.string.log_message_need_data_download,
            version.version
        )

        is LogMessage.Data.LatestVersionFound -> context.getString(
            R.string.log_message_latest_data_found,
            version.version
        )

        LogMessage.Data.UpdateSuccess -> context.getString(R.string.log_message_success_data_update)
        is LogMessage.Error -> {
            val message = when (this) {
                is LogMessage.GPS.StartFailure -> context.getString(R.string.log_message_gps_start_failure)
                is LogMessage.Data.CheckLatestVersionFailure -> context.getString(
                    R.string.log_message_latest_check_failure
                )
                is LogMessage.Data.UpdateFailure -> context.getString(R.string.log_message_fail_data_update)
            }
            "$message caused by;\n${error.formatStackTrace()}"
        }
    }

private fun String.trim(max: Int) = if (length > max) {
    substring(0, max) + " ...(and ${length - max}letters)"
} else {
    this
}

fun Throwable.formatStackTrace(): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    printStackTrace(pw)
    return sw.toString()
}
