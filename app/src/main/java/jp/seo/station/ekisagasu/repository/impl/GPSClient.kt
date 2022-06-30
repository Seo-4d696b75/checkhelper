package jp.seo.station.ekisagasu.repository.impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import jp.seo.station.ekisagasu.repository.LocationRepository
import jp.seo.station.ekisagasu.utils.LiveEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * @author Seo-4d696b75
 * @version 2020/12/23.
 */
class GPSClient(
    private val context: Context,
    defaultDispatcher: CoroutineDispatcher,
) : LocationCallback(), LocationRepository, CoroutineScope {

    private val job = Job()

    override val coroutineContext = defaultDispatcher + job

    private val locationClient = LocationServices.getFusedLocationProviderClient(context)
    private val settingClient = LocationServices.getSettingsClient(context)

    private var minInterval = 0

    private val _location = MutableSharedFlow<Location?>(replay = 0)

    private val _running: MutableLiveData<Boolean> = MutableLiveData(false)
    private var running = false
    override val messageLog = LiveEvent<String>()
    override val messageError = LiveEvent<String>()

    override val currentLocation = _location.asLiveData(context = coroutineContext)

    override val isRunning: LiveData<Boolean> = _running

    override val apiException = LiveEvent<ResolvableApiException>(true)

    override fun onLocationResult(result: LocationResult) {
        result.lastLocation?.let {
            launch { _location.emit(it) }
        }
    }

    override fun onLocationAvailability(p: LocationAvailability) {
        Log.d("GPS", "isLocationAvailable: ${p.isLocationAvailable}")
    }

    /**
     * @param interval  in seconds
     * @throws ResolvableApiException
     */
    override fun startWatchCurrentLocation(interval: Int) {
        if (interval < 1) return
        try {
            if (running) {
                if (interval != minInterval) {
                    log(
                        String.format(
                            "GPS > min interval %d>%d sec",
                            minInterval,
                            interval
                        )
                    )
                    minInterval = interval
                    locationClient.removeLocationUpdates(this)
                        .addOnCompleteListener {
                            running = false
                            requestGPSUpdate()
                        }
                }
            } else {
                minInterval = interval
                requestGPSUpdate()

                log(
                    String.format(
                        "GPS > min interval: %d sec",
                        minInterval
                    )
                )
            }
        } catch (e: ResolvableApiException) {
            apiException.postCall(e)
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
        settingClient.checkLocationSettings(settingRequest)
            .addOnSuccessListener {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationClient.requestLocationUpdates(request, this, Looper.getMainLooper())
                    running = true
                    _running.value = true
                } else {
                    error("permission denied: ACCESS_FILE_LOCATION", "Permission Denied")

                }
            }.addOnFailureListener { e ->
                if (e is ApiException && e.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    error("resolution required:" + e.message, "Please grant permission")
                    throw e as ResolvableApiException
                } else {
                    error("Fail to start GPS update: " + e.message, "Fail to start GPS")
                }
            }
    }

    override fun stopWatchCurrentLocation(): Boolean {
        if (running) {
            locationClient.removeLocationUpdates(this)
                .addOnCompleteListener {
                    running = false
                }

            _running.value = false
            launch { _location.emit(null) }
            log("GPS has stopped")
            job.cancel()
            return true
        }
        return false
    }

    private fun log(log: String) {
        messageLog.postCall(log)
    }

    private fun error(log: String, mes: String) {
        log(log)
        running = false
        _running.value = false
        messageError.postCall(mes)
    }

}

