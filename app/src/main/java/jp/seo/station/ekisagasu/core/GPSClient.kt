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

/**
 * @author Seo-4d696b75
 * @version 2020/12/23.
 */
class GPSClient(ctx: Context, private var callback: GPSCallback?) : LocationCallback() {

    private val locationClient = LocationServices.getFusedLocationProviderClient(ctx)
    private val settingClient = LocationServices.getSettingsClient(ctx)

    private var context: Context? = ctx

    private val tags: MutableSet<String> = HashSet()
    private var minInterval = 0

    private val _location: MutableLiveData<Location?> = MutableLiveData(null)
    private val _running: MutableLiveData<Boolean> = MutableLiveData(false)
    private var running = false

    val currentLocation: LiveData<Location?>
        get() = _location

    val isRunning: LiveData<Boolean>
        get() = _running

    override fun onLocationResult(result: LocationResult?) {
        result?.lastLocation?.let {
            callback?.onLocationUpdated(it)
            _location.value = it
        }
    }

    override fun onLocationAvailability(p: LocationAvailability?) {
        Log.d("GPS", "isLocationAvailable: " + (p?.isLocationAvailable ?: false))
    }

    fun release() {
        if (running) {
            locationClient.removeLocationUpdates(this)
                .addOnCompleteListener {
                    callback?.onGPSStop("GPS client released")
                    _running.value = false
                    _location.value = null
                    running = false
                    release()
                }
            return
        }
        callback = null
        context = null
    }

    /**
     * @param interval  in seconds
     * @throws ResolvableApiException
     */
    fun requestGPSUpdate(interval: Int, tag: String) {
        if (interval < 1) return
        if (running) {
            if (interval != minInterval) {
                callback?.onGPSLog(
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
                        requestGPSUpdate(tag)
                    }
            }
        } else {
            minInterval = interval
            requestGPSUpdate(tag)
        }
    }


    private fun requestGPSUpdate(tag: String) {
        callback?.onGPSLog(
            String.format(
                "GPS > %s requests update. min interval: %d sec",
                tag,
                minInterval
            )
        )
        tags.add(tag)
        val request = LocationRequest()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(minInterval * 1000L)
            .setFastestInterval(minInterval * 1000L)
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

    fun stopGPSUpdate(tag: String) {
        tags.remove(tag)
        if (tags.isEmpty()) {
            locationClient.removeLocationUpdates(this)
                .addOnCompleteListener {
                    running = false
                    _running.value = false
                    _location.value = null
                }
        }
    }

    private fun log(log: String) {
        callback?.onGPSLog(log)
    }

    private fun error(log: String, mes: String) {
        log(log)
        running = false
        _running.value = false
        callback?.onGPSStop(mes)
    }

}

interface GPSCallback {
    fun onLocationUpdated(location: Location)
    fun onGPSStop(mes: String)
    fun onGPSLog(log: String)
}
