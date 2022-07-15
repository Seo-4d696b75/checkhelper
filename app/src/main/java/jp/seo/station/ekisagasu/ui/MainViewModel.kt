package jp.seo.station.ekisagasu.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.AppLogger
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.repository.DataRepository
import jp.seo.station.ekisagasu.ui.dialog.DataUpdateType
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val dataRepository: DataRepository,
    appLogger: AppLogger,
) : ViewModel(), AppLogger by appLogger {
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

    fun onActivityResultResolved(requestCode: Int, resultCode: Int, data: Intent?) =
        viewModelScope.launch {
            appStateRepository.emitMessage(
                AppMessage.ReceiveActivityResult(
                    requestCode,
                    resultCode,
                    data
                )
            )
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
                    dataRepository.getLatestDataVersion(false)
                } catch (e: IOException) {
                    // TODO toast表示
                    appStateRepository.hasDataVersionChecked = false
                    return@launch
                }

                if (info == null) {
                    appStateRepository.emitMessage(
                        AppMessage.RequestDataUpdate(
                            type = DataUpdateType.Init,
                            info = latest,
                        )
                    )
                } else {
                    log(String.format("data found version:${info.version}"))
                    if (info.version < latest.version) {
                        appStateRepository.emitMessage(
                            AppMessage.RequestDataUpdate(
                                type = DataUpdateType.Latest,
                                info = latest,
                            )
                        )
                    }
                }

            }
        }
    }
}
