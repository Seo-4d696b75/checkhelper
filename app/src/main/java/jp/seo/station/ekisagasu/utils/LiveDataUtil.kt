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
                val liveData1Value = liveData1.value
                val liveData2Value = liveData2.value
                if (liveData1Value != null && liveData2Value != null) {
                    value = block(liveData1Value, liveData2Value)
                }
            }
        }
    }.distinctUntilChanged()
}

/**
 * 与えられた[value]が型パラメータ[E]に合致する場合にのみブロックを実行する
 *
 * 型パラメータ[E]に関して、
 * - NotNull: `value != null`の場合に実行
 * - Nullable: 必ず実行
 */
inline fun <reified E : Any?> getValueOrNull(value: E?, block: (E) -> Unit) {
    if (value != null) {
        block(value)
    } else if (null is E) {
        val nullValue = null as E
        block(nullValue)
    }
}

inline fun <T : Any?, reified LIVE1 : Any?, reified LIVE2 : Any?> combineLiveData(
    initialValue: T,
    liveData1: LiveData<LIVE1>,
    liveData2: LiveData<LIVE2>,
    crossinline block: (LIVE1, LIVE2) -> T
): LiveData<T> {
    return MediatorLiveData<T>().apply {
        value = initialValue
        listOf(liveData1, liveData2).forEach { liveData ->
            addSource(liveData) {
                getValueOrNull<LIVE1>(liveData1.value) { value1 ->
                    getValueOrNull<LIVE2>(liveData2.value) { value2 ->
                        value = block(value1, value2)
                    }
                }
            }
        }
    }.distinctUntilChanged()
}

data class CurrentLocation(
    val location: Location?,
    val k: Int
)
