package com.seo4d696b75.android.ekisagasu.data.permission

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.seo4d696b75.android.ekisagasu.domain.log.LogCollector
import com.seo4d696b75.android.ekisagasu.domain.log.LogMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.domain.permission.PermissionRepository
import com.seo4d696b75.android.ekisagasu.domain.permission.PermissionRepository.Companion.NOTIFICATION_CHANNEL_ID
import com.seo4d696b75.android.ekisagasu.domain.permission.PermissionState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PermissionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appStateRepository: AppStateRepository,
    private val store: PermissionDataStore,
    collector: LogCollector,
) : PermissionRepository, LogCollector by collector {

    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val client: SettingsClient by lazy {
        LocationServices.getSettingsClient(context)
    }

    override suspend fun getLocationPermissionState(): PermissionState {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return if (granted) {
            PermissionState.Granted
        } else {
            log(LogMessage.GPS.NoPermission)
            PermissionState.NotGranted(
                permission = Manifest.permission.ACCESS_FINE_LOCATION,
                hasDenied = store.hasLocationPermissionDenied(),
            )
        }
    }

    override suspend fun setLocationPermissionDenied() {
        store.setLocationPermissionDenied()
    }

    override suspend fun getNotificationPermissionState(): PermissionState {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            PermissionState.Granted
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                PermissionState.Granted
            } else {
                PermissionState.NotGranted(
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    hasDenied = store.hasNotificationPermissionDenied(),
                )
            }
        }
    }

    override suspend fun setNotificationPermissionDenied() {
        store.setNotificationPermissionDenied()
    }

    override val isDeviceLocationEnabled: Boolean
        get() = run {
            LocationManagerCompat.isLocationEnabled(locationManager)
        }.also {
            if (!it) {
                log(LogMessage.GPS.NoPermission)
            }
        }

    override suspend fun checkDeviceLocationSettings(minInterval: Int): Boolean {
        val request = LocationRequest.create().apply {
            priority = Priority.PRIORITY_HIGH_ACCURACY
            interval = minInterval * 1000L
            fastestInterval = minInterval * 1000L
        }
        val settingRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(request)
            .build()
        return suspendCoroutine { continuation ->
            client
                .checkLocationSettings(settingRequest)
                .addOnSuccessListener {
                    continuation.resume(true)
                }
                .addOnFailureListener { e ->
                    Timber.w(e)
                    if (e is ResolvableApiException && e.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                        log(LogMessage.GPS.ResolvableException)
                        appStateRepository.emitMessage(AppMessage.ResolvableException(e))
                    } else {
                        log(LogMessage.GPS.StartFailure(e))
                    }
                    continuation.resume(false)
                }
                .addOnCanceledListener {
                    continuation.resume(false)
                }
        }
    }

    override val isNotificationChannelEnabled: Boolean
        get() {
            if (!notificationManager.areNotificationsEnabled()) {
                return false
            }
            // no channel group
            val channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            return channel.importance != NotificationManager.IMPORTANCE_NONE
        }

    override val canDrawOverlay: Boolean
        get() = Settings.canDrawOverlays(context)
}

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface PermissionRepositoryModule {
    @Binds
    fun bind(impl: PermissionRepositoryImpl): PermissionRepository
}
