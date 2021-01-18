package jp.seo.station.ekisagasu.utils

import android.location.Location
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.distinctUntilChanged

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */


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


inline fun <T : Any> reduceLiveData(
    initialValue: T,
    vararg liveData: LiveData<in T>,
    crossinline accumulator: (acc: T, value: T) -> T
): LiveData<T> {
    return MediatorLiveData<T>().apply {
        value = initialValue
        liveData.forEach { e ->
            addSource(e) {
                value = liveData.mapNotNull {
                    @Suppress("UNCHECKED_CAST")
                    it.value as T?
                }.reduce(accumulator)
            }
        }
    }.distinctUntilChanged()
}

/**
 * [LiveData.observe]と同様だが最初の現在値でコールバックしない
 */
inline fun <reified T : Any?> LiveData<T>.onChanged(owner: LifecycleOwner, observer: Observer<T>) {
    getValueOrNull(this.value) { current ->
        var changed = false
        this.observe(owner) { value ->
            if (changed || current != value) {
                changed = true
                observer.onChanged(value)
            }
        }
    }
}

