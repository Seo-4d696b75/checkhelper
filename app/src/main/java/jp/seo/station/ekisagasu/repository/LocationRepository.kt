package jp.seo.station.ekisagasu.repository

import android.location.Location
import androidx.lifecycle.LiveData
import com.google.android.gms.common.api.ResolvableApiException
import jp.seo.station.ekisagasu.utils.LiveEvent

/**
 * 現在位置を取得する機能を抽象化
 */
interface LocationRepository {
    val currentLocation: LiveData<Location?>
    val isRunning: LiveData<Boolean>
    val apiException: LiveEvent<ResolvableApiException>
    val messageLog: LiveEvent<String>
    val messageError: LiveEvent<String>
    fun startWatchCurrentLocation(interval: Int)
    fun stopWatchCurrentLocation(): Boolean
}