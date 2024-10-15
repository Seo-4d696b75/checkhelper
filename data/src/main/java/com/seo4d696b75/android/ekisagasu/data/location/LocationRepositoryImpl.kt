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
import com.seo4d696b75.android.ekisagasu.domain.location.LocationState
import com.seo4d696b75.android.ekisagasu.domain.log.LogCollector
import com.seo4d696b75.android.ekisagasu.domain.log.LogMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.domain.permission.PermissionRepository
import com.seo4d696b75.android.ekisagasu.domain.permission.PermissionState
import com.seo4d696b75.android.ekisagasu.domain.user.UserSettingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val settingRepository: UserSettingRepository,
    private val logger: LogCollector,
) : LocationCallback(),
    LocationRepository,
    LogCollector by logger {

    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    private val stateFlow = MutableStateFlow<LocationState>(LocationState.Idle)

    private val mutex = Mutex()

    override val currentLocation = channelFlow {
        launch {
            settingRepository
                .setting
                .map { it.locationUpdateInterval }
                .distinctUntilChanged()
                .collectLatest {
                    if (stateFlow.value is LocationState.Running) {
                        startWatchCurrentLocation(it)
                    }
                }
        }
        stateFlow.collect(this::send)
    }

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
            stateFlow.update { current ->
                require(current is LocationState.Running)
                current.copy(location = model)
            }
        }
    }

    override fun onLocationAvailability(p: LocationAvailability) {
        Timber.d("isLocationAvailable: ${p.isLocationAvailable}")
    }

    override suspend fun startWatchCurrentLocation() {
        val interval = settingRepository.setting.first().locationUpdateInterval
        startWatchCurrentLocation(interval)
    }

    /**
     * 現在位置の監視を開始する
     *
     * - まだ開始されていない：新たに監視を開始
     * - 既に開始されている：指定されたintervalが現在値と異なる場合は再度スタートする
     */
    private suspend fun startWatchCurrentLocation(interval: Int) = mutex.withLock {
        if (interval < 1) return
        try {
            val current = stateFlow.value
            if (current is LocationState.Running) {
                if (interval != current.interval) {
                    log(LogMessage.GPS.IntervalChanged(current.interval, interval))
                    Timber.d("minInterval %d > %d", current.interval, interval)
                    removeLocationUpdate()
                    requestGPSUpdate(interval)
                }
            } else {
                log(LogMessage.GPS.Start(interval))
                Timber.d("GPS start")
                requestGPSUpdate(interval)
            }
        } catch (e: ResolvableApiException) {
            Timber.w(e)
            log(LogMessage.GPS.ResolvableException)
            appStateRepository.emitMessage(AppMessage.ResolvableException(e))
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestGPSUpdate(interval: Int) {
        if (
            !permissionRepository.isDeviceLocationEnabled ||
            permissionRepository.getLocationPermissionState() !is PermissionState.Granted ||
            !permissionRepository.checkDeviceLocationSettings(interval)
        ) {
            return
        }
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            interval * 1000L,
        )
            .setMinUpdateIntervalMillis(interval * 1000L)
            .build()
        locationClient.requestLocationUpdates(request, this, Looper.getMainLooper())
        stateFlow.update {
            when (it) {
                LocationState.Idle -> LocationState.Running(null, interval)
                is LocationState.Running -> it.copy(interval = interval)
            }
        }
    }

    override suspend fun stopWatchCurrentLocation(): Boolean = mutex.withLock {
        if (stateFlow.value is LocationState.Running) {
            removeLocationUpdate()
            Timber.d("GPS stop")
            stateFlow.update { LocationState.Idle }
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
