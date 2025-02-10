package com.seo4d696b75.android.ekisagasu.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.RemoteDataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateType
import com.seo4d696b75.android.ekisagasu.domain.error.CheckLatestDataVersionException
import com.seo4d696b75.android.ekisagasu.domain.error.ErrorHandler
import com.seo4d696b75.android.ekisagasu.domain.log.LogCollector
import com.seo4d696b75.android.ekisagasu.domain.log.LogMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.ui.MainActivity.Companion.INTENT_KEY_SELECT_NAVIGATION
import com.seo4d696b75.android.ekisagasu.ui.selectLine.LineSelectType
import com.seo4d696b75.android.ekisagasu.ui.selectLine.NavigateSelectLineEvent
import com.seo4d696b75.android.ekisagasu.ui.update.NavigateDataUpdateEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.take
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val dataRepository: DataRepository,
    private val remoteDataRepository: RemoteDataRepository,
    private val navigateDataUpdateEvent: NavigateDataUpdateEvent,
    private val selectLine: NavigateSelectLineEvent,
    logger: LogCollector,
    errorHandler: ErrorHandler,
) : ViewModel(),
    LogCollector by logger,
    ErrorHandler by errorHandler {

    val appFinish = appStateRepository.appFinish.take(1)

    var isServiceRunning: Boolean
        get() = appStateRepository.isServiceRunning
        set(value) {
            appStateRepository.isServiceRunning = value
        }

    /**
     * アプリに必要なデータを確認
     * (1) check data version
     * (2) check data initialized
     */
    fun checkData() {
        // check data version
        if (!appStateRepository.hasDataVersionChecked) {
            appStateRepository.hasDataVersionChecked = true

            viewModelScope.launchCatching {
                val info = dataRepository.getDataVersion()

                val latest = runCatching {
                    remoteDataRepository.getLatestDataVersion(true)
                }.onFailure { e ->
                    log(LogMessage.Data.CheckLatestVersionFailure(e))
                    appStateRepository.hasDataVersionChecked = false
                    throw CheckLatestDataVersionException(e)
                }.getOrThrow()

                if (info == null) {
                    Timber.d("no data saved, download required")
                    log(LogMessage.Data.DownloadRequired(latest))
                    navigateDataUpdateEvent(DataUpdateType.Init, latest)
                } else {
                    log(LogMessage.Data.Found(info))
                    if (info.version < latest.version) {
                        log(LogMessage.Data.LatestVersionFound(latest))
                        navigateDataUpdateEvent(DataUpdateType.Latest, latest)
                    }
                }
            }
        }
    }

    fun onIntent(intent: Intent?) {
        intent ?: return
        if (intent.getBooleanExtra(INTENT_KEY_SELECT_NAVIGATION, false)) {
            intent.putExtra(INTENT_KEY_SELECT_NAVIGATION, false)
            selectLine(LineSelectType.Navigator)
        }
    }
}
