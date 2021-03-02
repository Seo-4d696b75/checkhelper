package jp.seo.station.ekisagasu.core

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import jp.seo.station.ekisagasu.utils.LiveEvent

/**
 * @author Seo-4d696b75
 * @version 2020/12/23.
 */
class GPSClient(ctx: Context) : LocationCallback() {

    private val locationClient = LocationServices.getFusedLocationProviderClient(ctx)
    private val settingClient = LocationServices.getSettingsClient(ctx)

    private var context: Context? = ctx

    private val requests: MutableMap<String, GPSRequest> = HashMap()
    private var minInterval = 0

    private val _location: MutableLiveData<Location?> = MutableLiveData(null)
    private val _running: MutableLiveData<Boolean> = MutableLiveData(false)
    private var running = false
    val messageLog = LiveEvent<String>()
    val messageError = LiveEvent<String>(true)

    val currentLocation: LiveData<Location?> = _location

    val isRunning: LiveData<Boolean> = _running

    val apiException = LiveEvent<ResolvableApiException>(true)

    override fun onLocationResult(result: LocationResult?) {
        result?.lastLocation?.let { loc ->
            //Log.d("GPS", String.format("%.6f/%.6f", it.latitude, it.longitude))
            _location.value = loc
        }
    }

    override fun onLocationAvailability(p: LocationAvailability?) {
        Log.d("GPS", "isLocationAvailable: " + (p?.isLocationAvailable ?: false))
    }

    /**
     * @param interval  in seconds
     * @throws ResolvableApiException
     */
    fun requestGPSUpdate(interval: Int, tag: String) {
        if (interval < 1) return
        try {
            requests[tag] = GPSRequest(tag, interval)
            if (running) {
                if (interval < minInterval) {
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
                } else {
                    log(
                        String.format(
                            "GPS > min interval request:%d remained:%d sec",
                            interval,
                            minInterval
                        )
                    )
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


    private fun requestGPSUpdate() {
        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = minInterval * 1000L
            fastestInterval = minInterval * 1000L
        }
        val settingRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(request)
            .build()
        settingClient.checkLocationSettings(settingRequest)
            .addOnSuccessListener {
                context?.let {
                    if (ContextCompat.checkSelfPermission(
                            it,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        locationClient.requestLocationUpdates(request, this, Looper.getMainLooper())
                        running = true
                        _running.value = true
                    } else {
                        error("permission denied: ACCESS_FILE_LOCATION", "Permission Denied")
                    }
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

    fun stopGPSUpdate(tag: String): Boolean {
        if (requests.remove(tag) != null) {
            if (requests.isEmpty()) {
                locationClient.removeLocationUpdates(this)
                    .addOnCompleteListener {
                        running = false
                    }

                _running.value = false
                _location.value = null
                log("GPS has stopped")
            } else {
                val min = requests.values.minOf { it.minInterval }
                if (min > minInterval) {
                    log(
                        String.format(
                            "GPS > min interval %d>%d sec",
                            minInterval,
                            min
                        )
                    )
                    minInterval = min
                    locationClient.removeLocationUpdates(this)
                        .addOnCompleteListener {
                            running = false
                            requestGPSUpdate()
                        }
                }
            }
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

    private data class GPSRequest(
        val tag: String,
        val minInterval: Int
    )

}

