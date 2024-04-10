package jp.seo.station.ekisagasu.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seo4d696b75.android.ekisagasu.domain.log.LogCollector
import com.seo4d696b75.android.ekisagasu.domain.log.LogMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateType
import com.seo4d696b75.android.ekisagasu.domain.dataset.RemoteDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val dataRepository: DataRepository,
    private val remoteDataRepository: RemoteDataRepository,
    private val logger: LogCollector,
) : ViewModel(),
    LogCollector by logger {
    val message = appStateRepository.message

    var hasPermissionChecked: Boolean
        get() = appStateRepository.hasPermissionChecked
        set(value) {
            appStateRepository.hasPermissionChecked = value
        }

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

            viewModelScope.launch {
                val info = dataRepository.getDataVersion()

                val latest = try {
                    remoteDataRepository.getLatestDataVersion(true)
                } catch (e: IOException) {
                    Timber.w(e)
                    log(LogMessage.Data.CheckLatestVersionFailure(e))
                    appStateRepository.emitMessage(AppMessage.Data.CheckLatestVersionFailure(e))
                    appStateRepository.hasDataVersionChecked = false
                    return@launch
                }

                if (info == null) {
                    Timber.d("no data saved, download required")
                    log(LogMessage.Data.DownloadRequired(latest))
                    appStateRepository.emitMessage(
                        AppMessage.Data.ConfirmUpdate(
                            type = DataUpdateType.Init,
                            info = latest,
                        ),
                    )
                } else {
                    log(LogMessage.Data.Found(info))
                    if (info.version < latest.version) {
                        log(LogMessage.Data.LatestVersionFound(latest))
                        appStateRepository.emitMessage(
                            AppMessage.Data.ConfirmUpdate(
                                type = DataUpdateType.Latest,
                                info = latest,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun requestAppFinish() = viewModelScope.launch {
        appStateRepository.emitMessage(
            AppMessage.FinishApp,
        )
    }
}
