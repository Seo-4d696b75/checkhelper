package jp.seo.station.ekisagasu.utils

import android.location.Location
import androidx.annotation.MainThread
import androidx.lifecycle.*

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
@MainThread
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

/**
 * 指定したタグで識別されるObserverごと一回のみコールする
 */
open class LiveEvent<E>(
    /**
     * `observe, observeForever`でオブザーバを登録したタイミングでLiveDataにキャッシュされている
     * 最新のイベントを通知しない場合は`true`を指定する
     */
    private val skipCachedEvent: Boolean = false
) {

    private val liveData = MutableLiveData<EventWrapper<E>>()

    private var _version = 0L

    private fun lastVersion(): Long = synchronized(this) {
        return if (skipCachedEvent) _version else _version - 1
    }

    private fun incrementVersion(): Long = synchronized(this) {
        _version++
        return _version
    }

    fun observe(owner: LifecycleOwner, observer: Observer<E>) {
        liveData.observe(owner, ObserverWrapper(observer, lastVersion()))
    }

    fun observeForever(observer: Observer<E>) {
        liveData.observeForever(ObserverWrapper(observer, lastVersion()))
    }

    @MainThread
    fun call(event: E) {
        liveData.value = EventWrapper(event, incrementVersion())
    }

    fun postCall(event: E) {
        liveData.postValue(EventWrapper(event, incrementVersion()))
    }

    private data class EventWrapper<E>(
        val value: E,
        val version: Long
    )

    private class ObserverWrapper<E>(
        val original: Observer<E>,
        private var lastVersion: Long
    ) : Observer<EventWrapper<E>> {
        override fun onChanged(t: EventWrapper<E>) {
            if (t.version > lastVersion) {
                lastVersion = t.version
                original.onChanged(t.value)
            }
        }

    }

}

class UnitLiveEvent(skipCachedEvent: Boolean = false) : LiveEvent<Unit>(skipCachedEvent) {

    fun call() {
        call(Unit)
    }

    fun postCall() {
        postCall(Unit)
    }

}

