package jp.seo.station.ekisagasu.ui.log

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.data.database.AppLog
import com.seo4d696b75.android.ekisagasu.data.gpx.SerializeGPXUseCase
import com.seo4d696b75.android.ekisagasu.data.log.AppLogType
import com.seo4d696b75.android.ekisagasu.data.log.LogRepository
import com.seo4d696b75.android.ekisagasu.data.message.AppMessage
import com.seo4d696b75.android.ekisagasu.data.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.data.station.DataRepository
import com.seo4d696b75.android.ekisagasu.data.utils.TIME_PATTERN_DATETIME
import com.seo4d696b75.android.ekisagasu.data.utils.TIME_PATTERN_DATETIME_FILE
import com.seo4d696b75.android.ekisagasu.data.utils.TIME_PATTERN_MILLI_SEC
import com.seo4d696b75.android.ekisagasu.data.utils.formatTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    logRepository: LogRepository,
    private val appStateRepository: AppStateRepository,
    private val dataRepository: DataRepository,
    private val serializeGPX: SerializeGPXUseCase,
) : ViewModel() {
    val target =
        logRepository.filter
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _filter = MutableStateFlow(AppLogType.Filter.All)

    fun setLogFilter(filter: AppLogType.Filter) {
        _filter.value = filter
    }

    val logs: StateFlow<List<AppLog>> =
        combine(
            logRepository.logs,
            _filter,
        ) { logs, filter ->
            logs.filter { (it.type.value and filter.value) > 0 }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun requestWriteLog(
        appName: String,
        onConfigRequested: (defaultConfig: LogOutputConfig) -> Unit,
    ) {
        when (_filter.value) {
            AppLogType.Filter.All -> {
                requestLogOutput(appName, LogOutputConfig.All)
            }

            AppLogType.Filter.System -> {
                requestLogOutput(appName, LogOutputConfig.System)
            }

            AppLogType.Filter.Station -> {
                requestLogOutput(appName, LogOutputConfig.Station)
            }

            AppLogType.Filter.Geo -> {
                onConfigRequested(LogOutputConfig.Geo(LogOutputExtension.txt))
            }
        }
    }

    val onLogConfigResolved =
        appStateRepository.message
            .filterIsInstance<AppMessage.LogOutputConfigResolved>()

    fun requestLogOutput(
        appName: String,
        config: LogOutputConfig,
    ) = viewModelScope.launch {
        val time = Date()
        val type = _filter.value
        assert(config.filter == type)
        val list = logs.value

        val fileName =
            String.format(
                Locale.US,
                "%s_%sLog_%s.%s",
                appName,
                type.name,
                formatTime(TIME_PATTERN_DATETIME_FILE, time),
                config.extension.name.lowercase(),
            )
        fileContext =
            if (config.extension == LogOutputExtension.gpx) {
                serializeGPX(
                    log = list,
                    dataVersion = dataRepository.dataVersion.value?.version ?: throw RuntimeException(),
                )
            } else {
                StringBuilder().apply {
                    append(appName)
                    append("\nlog type : ")
                    append(type.name)
                    append("\nwritten time : ")
                    append(formatTime(TIME_PATTERN_DATETIME, time))
                    for (log in list) {
                        append("\n")
                        append(formatTime(TIME_PATTERN_MILLI_SEC, log.timestamp))
                        append(" ")
                        append(log.message)
                    }
                }.toString()
            }
        val intent =
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                this.type = "text/*"
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
        appStateRepository.emitMessage(
            AppMessage.StartActivityForResult(REQUEST_CODE_LOG_FILE_URI, intent),
        )
    }

    private var fileContext: String? = null

    val onLogFileUriResolved =
        appStateRepository.message
            .filterIsInstance<AppMessage.ReceiveActivityResult>()
            .filter {
                it.code == REQUEST_CODE_LOG_FILE_URI && it.result == Activity.RESULT_OK
            }.map {
                it.data?.data
            }.filterNotNull()

    fun writeLog(
        uri: Uri,
        resolver: ContentResolver,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val str = fileContext ?: return@launch
        try {
            resolver.openOutputStream(uri).use {
                val writer = BufferedWriter(OutputStreamWriter(it, Charsets.UTF_8))
                writer.write(str)
                writer.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        fileContext = null
    }

    companion object {
        const val REQUEST_CODE_LOG_FILE_URI = 20220713
    }
}
