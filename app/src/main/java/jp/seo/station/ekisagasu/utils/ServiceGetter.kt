package jp.seo.station.ekisagasu.utils

import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import jp.seo.station.ekisagasu.core.StationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * @author Seo-4d696b75
 * @version 2020/12/22.
 */
class ServiceGetter() : LifecycleObserver, CoroutineScope {

    private val job = Job()
    private var service: StationService? = null
    private var active = false
    private val listeners = ArrayList<(StationService) -> Unit>()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    val value: StationService?
        get() = service

    @Synchronized
    @MainThread
    fun get(block: (StationService) -> Unit) {
        val s = service
        if (s != null && active) {
            block(s)
        } else {
            listeners.add(block)
        }
    }

    @Synchronized
    fun set(s: StationService?) {
        service = s
        service?.let { s ->
            if (active) {
                launch {
                    listeners.forEach {
                        it(s)
                    }
                    listeners.clear()
                }
            }
        }
    }

    @Synchronized
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        active = true
        service?.let { s ->
            listeners.forEach { it(s) }
            listeners.clear()
        }
    }

    @Synchronized
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        job.cancel()
        active = false
    }
}
