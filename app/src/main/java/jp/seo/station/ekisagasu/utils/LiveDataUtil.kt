package jp.seo.station.ekisagasu.utils

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.distinctUntilChanged

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */

inline fun <T : Any?, LIVE1 : Any?, LIVE2 : Any?> combine(
    initialValue: T,
    liveData1: LiveData<LIVE1>,
    liveData2: LiveData<LIVE2>,
    crossinline block: (LIVE1, LIVE2) -> T
): LiveData<T> {
    return MediatorLiveData<T>().apply {
        value = initialValue
        listOf(liveData1, liveData2).forEach { liveData ->
            addSource(liveData) {
                val currentValue = value
                val liveData1Value = liveData1.value
                val liveData2Value = liveData2.value
                if (liveData1Value != null && liveData2Value != null) {
                    value = block(liveData1Value, liveData2Value)
                }
            }
        }
    }.distinctUntilChanged()
}

data class CurrentLocation(
    val location: Location?,
    val k: Int
)
