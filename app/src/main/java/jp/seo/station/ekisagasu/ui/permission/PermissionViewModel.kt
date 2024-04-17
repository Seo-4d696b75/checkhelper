package jp.seo.station.ekisagasu.ui.permission

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.seo4d696b75.android.ekisagasu.domain.permission.PermissionRepository
import com.seo4d696b75.android.ekisagasu.domain.permission.PermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionRepository: PermissionRepository,
) : ViewModel() {

    private val _hasChecked = MutableStateFlow(false)
    val hasChecked = _hasChecked.asStateFlow()

    sealed interface Event {
        sealed interface MissingRequirement : Event {
            data class LocationPermission(val state: PermissionState.NotGranted) : MissingRequirement
            data class NotificationPermission(val state: PermissionState.NotGranted) : MissingRequirement
            data class GooglePlayService(val errorCode: Int) : MissingRequirement
            data object DrawOverlay : MissingRequirement
            data object NotificationChannel : MissingRequirement
        }

        data object PermissionDenied : Event
        data class RequestPermission(val rationale: PermissionRationale) : Event
    }

    private val _event = MutableSharedFlow<Event>()
    val event = _event.asSharedFlow()

    private var hasAnyPermissionDenied = false
    private suspend fun onPermissionDenied() {
        if (!hasAnyPermissionDenied) {
            // 重複防止
            hasAnyPermissionDenied = true
            _event.emit(Event.PermissionDenied)
        }
    }

    // ActivityResultLauncher.launch で結果を受け取れない＆拒否された場合に繰り返さずアプリ終了させるフラグ
    private var hasLocationPermissionRequested = false
    private var hasNotificationPermissionRequested = false
    private var hasDrawOverlayRequested = false
    private var hasGooglePlayServiceRequested = false

    fun check() = viewModelScope.launch {
        // 端末の位置情報が有効化されているか
        if (!permissionRepository.isDeviceLocationEnabled) {
            // 失敗してシステムの位置情報許可ダイアログが表示される
            permissionRepository.checkDeviceLocationSettings(1)
            return@launch
        }

        // 位置情報の権限
        val location = permissionRepository.getLocationPermissionState()
        if (location is PermissionState.NotGranted) {
            if (hasLocationPermissionRequested) {
                onPermissionDenied()
            } else {
                hasLocationPermissionRequested = true
                _event.emit(Event.MissingRequirement.LocationPermission(location))
            }
            return@launch
        }

        // 通知権限
        val notification = permissionRepository.getNotificationPermissionState()
        if (notification is PermissionState.NotGranted) {
            if (hasNotificationPermissionRequested) {
                onPermissionDenied()
            } else {
                hasNotificationPermissionRequested = true
                _event.emit(Event.MissingRequirement.NotificationPermission(notification))
            }
            return@launch
        }

        // 通知チャネル
        if (!permissionRepository.isNotificationChannelEnabled) {
            if (hasNotificationPermissionRequested) {
                onPermissionDenied()
            } else {
                hasNotificationPermissionRequested = true
                _event.emit(Event.MissingRequirement.NotificationChannel)
            }
            return@launch
        }

        // 重ねて表示
        if (!permissionRepository.canDrawOverlay) {
            if (hasDrawOverlayRequested) {
                onPermissionDenied()
            } else {
                hasDrawOverlayRequested = true
                _event.emit(Event.MissingRequirement.DrawOverlay)
            }
            return@launch
        }

        // Google Play Services
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        if (code != ConnectionResult.SUCCESS) {
            if (hasGooglePlayServiceRequested) {
                // TODO より適切なUI表現
                onPermissionDenied()
            } else {
                hasGooglePlayServiceRequested = true
                _event.emit(Event.MissingRequirement.GooglePlayService(code))
            }
            return@launch
        }

        _hasChecked.update { true }
    }

    fun onDeviceLocationSettingResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) {
            viewModelScope.launch {
                onPermissionDenied()
            }
        }
    }

    fun onLocationPermissionResult(result: Boolean) {
        if (!result) {
            viewModelScope.launch {
                permissionRepository.setLocationPermissionDenied()
                onPermissionDenied()
            }
        }
    }

    fun onNotificationPermissionResult(result: Boolean) {
        if (!result) {
            viewModelScope.launch {
                permissionRepository.setNotificationPermissionDenied()
                onPermissionDenied()
            }
        }
    }

    fun onPermissionRequestCancelled() = viewModelScope.launch {
        onPermissionDenied()
    }

    fun requestPermission(rationale: PermissionRationale) = viewModelScope.launch {
        _event.emit(Event.RequestPermission(rationale))
    }
}
