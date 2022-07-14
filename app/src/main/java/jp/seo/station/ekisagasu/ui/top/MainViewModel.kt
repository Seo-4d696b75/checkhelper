package jp.seo.station.ekisagasu.ui.top

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.seo.station.ekisagasu.api.DataLatestInfo
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.AppLogger
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.repository.DataRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
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

    private val _event = MutableSharedFlow<MainViewEvent>()

    val event: SharedFlow<MainViewEvent> = _event

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
                    _event.emit(MainViewEvent.NeedUpdateData(latest))
                } else {
                    log(String.format("data found version:${info.version}"))
                    if (info.version < latest.version) {
                        _event.emit(MainViewEvent.LatestDataFound(latest))
                    }
                }

            }
        }
    }
}

sealed interface MainViewEvent {
    data class NeedUpdateData(val info: DataLatestInfo) : MainViewEvent
    data class LatestDataFound(val info: DataLatestInfo) : MainViewEvent
}
