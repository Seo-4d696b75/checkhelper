package com.seo4d696b75.android.ekisagasu.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.seo4d696b75.android.ekisagasu.domain.location.Location
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.log.LogCollector
import com.seo4d696b75.android.ekisagasu.domain.log.LogMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppMessage
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2020/12/23.
 */
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appStateRepository: AppStateRepository,
    private val logger: LogCollector,
) : LocationCallback(),
    LocationRepository,
    LogCollector by logger {

    private val locationClient = LocationServices.getFusedLocationProviderClient(context)
    private val settingClient = LocationServices.getSettingsClient(context)

    private var minInterval = 0

    private val _location = MutableStateFlow<Location?>(null)

    private val _running = MutableStateFlow(false)

    override val currentLocation = _location.asStateFlow()

    override val isRunning = _running.asStateFlow()

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
            _location.update { model }
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
    override fun startWatchCurrentLocation(interval: Int) {
        if (interval < 1) return
        try {
            if (_running.value) {
                if (interval != minInterval) {
                    log(LogMessage.GPS.IntervalChanged(minInterval, interval))
                    Timber.d("minInterval %d > %d", minInterval, interval)
                    minInterval = interval
                    locationClient.removeLocationUpdates(this)
                        .addOnCompleteListener {
                            _running.value = false
                            requestGPSUpdate()
                        }
                }
            } else {
                minInterval = interval
                requestGPSUpdate()

                log(LogMessage.GPS.Start(minInterval))
                Timber.d("GPS start")
            }
        } catch (e: ResolvableApiException) {
            Timber.w(e)
            log(LogMessage.GPS.ResolvableException)
            appStateRepository.emitMessage(AppMessage.ResolvableException(e))
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestGPSUpdate() {
        val request = LocationRequest.create().apply {
            priority = Priority.PRIORITY_HIGH_ACCURACY
            interval = minInterval * 1000L
            fastestInterval = minInterval * 1000L
        }
        val settingRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(request)
            .build()
        settingClient
            .checkLocationSettings(settingRequest)
            .addOnSuccessListener {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationClient.requestLocationUpdates(request, this, Looper.getMainLooper())
                    _running.value = true
                } else {
                    log(LogMessage.GPS.NoPermission)
                }
            }.addOnFailureListener { e ->
                Timber.w(e)
                if (e is ResolvableApiException && e.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    log(LogMessage.GPS.ResolvableException)
                    appStateRepository.emitMessage(AppMessage.ResolvableException(e))
                } else {
                    log(LogMessage.GPS.StartFailure(e))
                }
            }
    }

    override fun stopWatchCurrentLocation(): Boolean {
        if (_running.value) {
            locationClient
                .removeLocationUpdates(this)
                .addOnCompleteListener {
                    Timber.d("GPS stop")
                    _running.value = false
                    log(LogMessage.GPS.Stop)
                }
            return true
        }
        return false
    }
}
