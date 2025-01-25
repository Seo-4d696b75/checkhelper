package com.seo4d696b75.android.ekisagasu.ui.log

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.config.AppConfig
import com.seo4d696b75.android.ekisagasu.domain.date.TIME_PATTERN_DATETIME_FILE
import com.seo4d696b75.android.ekisagasu.domain.date.format
import com.seo4d696b75.android.ekisagasu.domain.error.ErrorHandler
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
import com.seo4d696b75.android.ekisagasu.domain.log.LogRepository
import com.seo4d696b75.android.ekisagasu.domain.log.filter
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEvent
import com.seo4d696b75.android.ekisagasu.ui.event.NavigationEventHolder
import com.seo4d696b75.android.ekisagasu.ui.event.navigationEventHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class LogComposeViewModel @Inject constructor(
    private val logRepository: LogRepository,
    private val appConfig: AppConfig,
    private val logSerializer: LogSerializer,
    private val gpxSerializer: GPXSerializer,
    handler: ErrorHandler,
) : ViewModel(),
    ErrorHandler by handler,
    NavigationEventHolder<LogComposeViewModel.Nav> by navigationEventHolder() {

    private val filter = MutableStateFlow(AppLogType.Filter.All)

    val uiState = combine(
        filter,
        logRepository.target,
        logRepository.logs,
    ) { filter, target, logs ->
        LogUiState.Loaded(
            filter = filter,
            target = target,
            logs = logs.filter(filter).toPersistentList(),
        )
    }.stateInCatching(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        LogUiState.Initializing,
    )

    fun onFilterChanged(filter: AppLogType.Filter) {
        this.filter.update { filter }
    }

    fun onSelectTargetClicked() {
        navigate(Nav.SelectLogTarget)
    }

    fun onSaveClicked() {
        val filter = this.filter.value
        val now = Date()
        val baseName = "${appConfig.appName}_${filter.name}Log_${now.format(TIME_PATTERN_DATETIME_FILE)}"
        val config = when (filter) {
            AppLogType.Filter.All -> LogOutputConfig.All(now.time, baseName)
            AppLogType.Filter.System -> LogOutputConfig.System(now.time, baseName)
            AppLogType.Filter.Geo -> LogOutputConfig.Geo(now.time, baseName)
            AppLogType.Filter.Station -> LogOutputConfig.Station(now.time, baseName)
        }
        if (config is LogOutputConfig.Geo) {
            navigate(Nav.ConfigureOutputFile(config))
        } else {
            onLogOutputConfigured(config)
        }
    }

    fun onLogOutputConfigured(config: LogOutputConfig) {
        navigate(Nav.RequestOutputFile(config))
    }

    fun writeLogFile(
        config: LogOutputConfig,
        uri: Uri,
        resolver: ContentResolver,
    ) = viewModelScope.launchCatching(Dispatchers.IO) {
        val state = uiState.value as? LogUiState.Loaded ?: throw IllegalStateException()
        requireNotNull(resolver.openOutputStream(uri)).let { stream ->
            when (config.extension) {
                LogOutputExtension.TXT -> {
                    logSerializer(config, state.logs, stream)
                }

                LogOutputExtension.GPX -> {
                    gpxSerializer(state.logs, stream)
                }
            }
        }
    }

    sealed interface Nav : NavigationEvent {
        data object SelectLogTarget : Nav
        data class RequestOutputFile(
            val config: LogOutputConfig,
        ) : Nav

        data class ConfigureOutputFile(
            val config: LogOutputConfig.Geo,
        ) : Nav
    }
}
