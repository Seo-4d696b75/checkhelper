package jp.seo.station.ekisagasu.ui.log

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.config.AppConfig
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.date.TIME_PATTERN_DATETIME
import com.seo4d696b75.android.ekisagasu.domain.date.TIME_PATTERN_DATETIME_FILE
import com.seo4d696b75.android.ekisagasu.domain.date.TIME_PATTERN_MILLI_SEC
import com.seo4d696b75.android.ekisagasu.domain.date.format
import com.seo4d696b75.android.ekisagasu.domain.log.AppLog
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
import com.seo4d696b75.android.ekisagasu.domain.log.LogRepository
import com.seo4d696b75.android.ekisagasu.domain.log.filter
import com.seo4d696b75.android.ekisagasu.domain.xml.GPXSerializer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
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
    private val appConfig: AppConfig,
    private val dataRepository: DataRepository,
    private val gpxSerializer: GPXSerializer,
) : ViewModel() {
    val target = logRepository
        .target
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val _filter = MutableStateFlow(AppLogType.Filter.All)

    fun setLogFilter(filter: AppLogType.Filter) {
        _filter.value = filter
    }

    val logs: StateFlow<List<AppLog>> = combine(
        logRepository.logs,
        _filter,
    ) { logs, filter ->
        logs.filter(filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun requestWriteLog(
        onConfigRequested: (defaultConfig: LogOutputConfig) -> Unit,
    ) {
        when (_filter.value) {
            AppLogType.Filter.All -> {
                requestLogOutput(LogOutputConfig.All)
            }

            AppLogType.Filter.System -> {
                requestLogOutput(LogOutputConfig.System)
            }

            AppLogType.Filter.Station -> {
                requestLogOutput(LogOutputConfig.Station)
            }

            AppLogType.Filter.Geo -> {
                onConfigRequested(LogOutputConfig.Geo(LogOutputExtension.txt))
            }
        }
    }

    private val _outputFileRequested = MutableSharedFlow<Intent>()
    val outputFileRequested = _outputFileRequested.asSharedFlow()

    fun requestLogOutput(config: LogOutputConfig) = viewModelScope.launch {
        val time = Date()
        val type = _filter.value
        assert(config.filter == type)
        val list = logs.value

        val fileName = String.format(
            Locale.US,
            "%s_%sLog_%s.%s",
            appConfig.appName,
            type.name,
            time.format(TIME_PATTERN_DATETIME_FILE),
            config.extension.name.lowercase(),
        )
        fileContent = if (config.extension == LogOutputExtension.gpx) {
            gpxSerializer(
                log = list,
                dataVersion = dataRepository.dataVersion.value?.version ?: throw RuntimeException(),
            )
        } else {
            StringBuilder().apply {
                append(appConfig.appName)
                append("\nlog type : ")
                append(type.name)
                append("\nwritten time : ")
                append(time.format(TIME_PATTERN_DATETIME))
                for (log in list) {
                    append("\n")
                    append(log.timestamp.format(TIME_PATTERN_MILLI_SEC))
                    append(" ")
                    append(log.message)
                }
            }.toString()
        }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            this.type = "text/*"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        _outputFileRequested.emit(intent)
    }

    private var fileContent: String? = null

    fun onOutputFileResolved(
        uri: Uri,
        resolver: ContentResolver,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val str = fileContent ?: return@launch
        try {
            resolver.openOutputStream(uri).use {
                val writer = BufferedWriter(OutputStreamWriter(it, Charsets.UTF_8))
                writer.write(str)
                writer.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        fileContent = null
    }
}
