package jp.seo.station.ekisagasu.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.repository.DataRepository
import jp.seo.station.ekisagasu.ui.dialog.DataUpdateType
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appStateRepository: AppStateRepository,
    private val dataRepository: DataRepository,
) : ViewModel() {
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
                    Timber.tag("MainViewModel").i(
                        context.getString(R.string.message_need_data_download, latest.version),
                    )
                    appStateRepository.emitMessage(
                        AppMessage.RequestDataUpdate(
                            type = DataUpdateType.Init,
                            info = latest,
                        )
                    )
                } else {
                    Timber.tag("MainViewModel").i(
                        context.getString(R.string.message_saved_data_found, info.version)
                    )
                    if (info.version < latest.version) {
                        Timber.tag("MainViewModel").i(
                            context.getString(R.string.message_latest_data_found, latest.version)
                        )
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

    fun requestAppFinish() = viewModelScope.launch {
        appStateRepository.emitMessage(
            AppMessage.FinishApp
        )
    }
}
