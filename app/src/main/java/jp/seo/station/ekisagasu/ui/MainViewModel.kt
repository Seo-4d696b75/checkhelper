package jp.seo.station.ekisagasu.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.RemoteDataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateType
import com.seo4d696b75.android.ekisagasu.domain.log.LogCollector
import com.seo4d696b75.android.ekisagasu.domain.log.LogMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.domain.permission.PermissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val appStateRepository: AppStateRepository,
    private val dataRepository: DataRepository,
    private val remoteDataRepository: RemoteDataRepository,
    private val permissionRepository: PermissionRepository,
    private val logger: LogCollector,
) : ViewModel(),
    LogCollector by logger {

    sealed interface Event {
        data object LocationPermissionRequired : Event
        data class GooglePlayServiceRequired(val errorCode: Int) : Event
        data object DrawOverlayRequired : Event
        data object NotificationPermissionRequired : Event
        data object PermissionDenied : Event
    }

    private val _event = MutableSharedFlow<Event>()
    val event = _event.asSharedFlow()

    val message = appStateRepository.message

    var isServiceRunning: Boolean
        get() = appStateRepository.isServiceRunning
        set(value) {
            appStateRepository.isServiceRunning = value
        }

    private var hasDrawOverlayRequested = false
    private var hasGooglePlayServiceRequested = false

    // TODO permissionの必要をユーザ側に説明するUI
    fun checkPermission(): Boolean {
        if (!permissionRepository.isDeviceLocationEnabled) {
            viewModelScope.launch {
                // 失敗してシステムの位置情報許可ダイアログが表示される
                permissionRepository.checkDeviceLocationSettings(1)
            }
            return false
        }
        if (!permissionRepository.isLocationGranted) {
            viewModelScope.launch {
                _event.emit(Event.LocationPermissionRequired)
            }
            return false
        }
        // TODO notification channel も考慮する
        if (!permissionRepository.isNotificationGranted) {
            viewModelScope.launch {
                _event.emit(Event.NotificationPermissionRequired)
            }
            return false
        }
        if (!permissionRepository.canDrawOverlay) {
            viewModelScope.launch {
                if (hasDrawOverlayRequested) {
                    _event.emit(Event.PermissionDenied)
                } else {
                    hasDrawOverlayRequested = true
                    _event.emit(Event.DrawOverlayRequired)
                }
            }
            return false
        }
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        if (code != ConnectionResult.SUCCESS) {
            viewModelScope.launch {
                if (hasGooglePlayServiceRequested) {
                    // TODO より適切なUI表現
                    _event.emit(Event.PermissionDenied)
                } else {
                    hasGooglePlayServiceRequested = true
                    _event.emit(Event.GooglePlayServiceRequired(code))
                }
            }
            return false
        }
        return true
    }

    fun onPermissionResult(result: Boolean) {
        if (!result) {
            viewModelScope.launch {
                _event.emit(Event.PermissionDenied)
            }
        }
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
