package com.seo4d696b75.android.ekisagasu.ui.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface NavigationEventHolder<T : NavigationEvent> {
    /**
     * [NavigationEvent]を待ち行列に追加する
     */
    fun navigate(nav: T)

    /**
     * [lifecycle]に合わせてイベントを購読する
     *
     * - 複数のcollectorが存在する場合はひとつのみ購読可能
     * - 購読前に[navigate]で新しいイベントが追加された場合は最新のイベントのみ購読される
     */
    suspend fun collectNavigation(
        lifecycle: Lifecycle,
        minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
        collector: FlowCollector<T>,
    )
}

fun <T : NavigationEvent> navigationEventHolder(): NavigationEventHolder<T> = NavigationEventHolderImpl()

private class NavigationEventHolderImpl<T : NavigationEvent> : NavigationEventHolder<T> {

    private sealed interface EventState<out T> {
        data object Empty : EventState<Nothing>
        data class Queued<T>(
            val event: T,
            val id: Int,
            val consumed: Boolean = false,
        ) : EventState<T>
    }

    private val state = MutableStateFlow<EventState<T>>(EventState.Empty)
    private var counter = 0

    override fun navigate(nav: T) {
        state.update {
            EventState.Queued(
                event = nav,
                id = counter++,
            )
        }
    }

    override suspend fun collectNavigation(
        lifecycle: Lifecycle,
        minActiveState: Lifecycle.State,
        collector: FlowCollector<T>
    ) {
        state
            .flowWithLifecycle(lifecycle, minActiveState)
            .filterIsInstance<EventState.Queued<T>>()
            .filter { target ->
                val latest = state.getAndUpdate { current ->
                    if (current is EventState.Queued && current.id == target.id && !current.consumed) {
                        current.copy(consumed = true)
                    } else {
                        current
                    }
                }
                latest is EventState.Queued && latest.id == target.id && !latest.consumed
            }
            .map { it.event }
            .collect(collector)
    }
}

@Composable
fun <Event, Holder> NavigationEvent(
    holder: Holder,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: FlowCollector<Event>,
) where Event : NavigationEvent,
        Holder : NavigationEventHolder<Event> {
    val latestCollector by rememberUpdatedState(collector)
    LaunchedEffect(holder) {
        holder.collectNavigation(lifecycle, minActiveState) {
            latestCollector.emit(it)
        }
    }
}
