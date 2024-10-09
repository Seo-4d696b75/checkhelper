package com.seo4d696b75.android.ekisagasu.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.seo4d696b75.android.ekisagasu.domain.location.Location
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.log.LogCollector
import com.seo4d696b75.android.ekisagasu.domain.log.LogMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.domain.permission.PermissionRepository
import com.seo4d696b75.android.ekisagasu.domain.permission.PermissionState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * @author Seo-4d696b75
 * @version 2020/12/23.
 */
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appStateRepository: AppStateRepository,
    private val permissionRepository: PermissionRepository,
    private val logger: LogCollector,
) : LocationCallback(),
    LocationRepository,
    LogCollector by logger {

    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    private var minInterval = 0

    private val locationFlow = MutableStateFlow<Location?>(null)

    private val runningFlow = MutableStateFlow(false)

    override val currentLocation = locationFlow.asStateFlow()

    override val isRunning = runningFlow.asStateFlow()

    override fun onLocationResult(result: LocationResult) {
        result.lastLocation?.let {
            Timber.d("(%.6f,%.6f)", it.latitude, it.longitude)
            log(LogMessage.Location(it.latitude, it.longitude))
            val model = Location(
                lat = it.latitude,
                lng = it.longitude,
                timestamp = it.time,
                elapsedRealtimeMillis = it.elapsedRealtimeNanos / 1000_1000L,
            )
            locationFlow.update { model }
        }
    }

    override fun onLocationAvailability(p: LocationAvailability) {
        Timber.d("isLocationAvailable: ${p.isLocationAvailable}")
    }

    /**
     * 現在位置の監視を開始する
     *
     * - まだ開始されていない：新たに監視を開始
     * - 既に開始されている：指定されたintervalが現在値と異なる場合は再度スタートする
     * @param interval  in seconds
     * @throws ResolvableApiException
     */
    override suspend fun startWatchCurrentLocation(interval: Int) {
        if (interval < 1) return
        try {
            if (runningFlow.value) {
                if (interval != minInterval) {
                    log(LogMessage.GPS.IntervalChanged(minInterval, interval))
                    Timber.d("minInterval %d > %d", minInterval, interval)
                    minInterval = interval
                    removeLocationUpdate()
                    runningFlow.value = false
                    requestGPSUpdate()
                }
            } else {
                log(LogMessage.GPS.Start(interval))
                Timber.d("GPS start")
                minInterval = interval
                requestGPSUpdate()
            }
        } catch (e: ResolvableApiException) {
            Timber.w(e)
            log(LogMessage.GPS.ResolvableException)
            appStateRepository.emitMessage(AppMessage.ResolvableException(e))
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestGPSUpdate() {
        if (
            !permissionRepository.isDeviceLocationEnabled ||
            permissionRepository.getLocationPermissionState() !is PermissionState.Granted ||
            !permissionRepository.checkDeviceLocationSettings(minInterval)
        ) {
            return
        }
        val request = LocationRequest.create().apply {
            priority = Priority.PRIORITY_HIGH_ACCURACY
            interval = minInterval * 1000L
            fastestInterval = minInterval * 1000L
        }
        locationClient.requestLocationUpdates(request, this, Looper.getMainLooper())
        runningFlow.value = true
    }

    override suspend fun stopWatchCurrentLocation(): Boolean {
        if (runningFlow.value) {
            removeLocationUpdate()
            Timber.d("GPS stop")
            locationFlow.update { null }
            runningFlow.update { false }
            log(LogMessage.GPS.Stop)
            return true
        }
        return false
    }

    private suspend fun removeLocationUpdate(): Unit = suspendCancellableCoroutine { c ->
        locationClient
            .removeLocationUpdates(this)
            .addOnSuccessListener {
                c.resume(Unit)
            }
            .addOnFailureListener {
                Timber.w(it)
                c.cancel(it)
            }
            .addOnCanceledListener {
                c.cancel()
            }
    }
}

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface LocationRepositoryModule {
    @Binds
    @Singleton
    fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository
}
