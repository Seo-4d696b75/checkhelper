package com.seo4d696b75.android.ekisagasu.ui.error

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

interface ErrorHandler {
    fun CoroutineScope.launchCatching(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit,
    ): Job

    fun <T> Flow<T>.stateInCatching(
        scope: CoroutineScope,
        started: SharingStarted,
        initialValue: T,
    ): StateFlow<T>

    fun enqueueThrowable(error: Throwable)
}

class ErrorHandlerImpl @Inject constructor(
    private val holder: ErrorStateHolder,
) : ErrorHandler {

    override fun CoroutineScope.launchCatching(
        context: CoroutineContext,
        start: CoroutineStart,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = launch(
        context = context,
        start = start,
    ) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            enqueueThrowable(e)
        }
    }

    override fun <T> Flow<T>.stateInCatching(
        scope: CoroutineScope,
        started: SharingStarted,
        initialValue: T,
    ): StateFlow<T> = retry { e ->
        holder.enqueue(e)
        val nextConsumed = holder
            .errorState
            .filterIsInstance<ErrorState.Queued>()
            .filter { it.consumed }
            .first()
            .error
        nextConsumed == e
    }.stateIn(
        scope = scope,
        started = started,
        initialValue = initialValue,
    )

    override fun enqueueThrowable(error: Throwable) {
        Timber.w(error, "caught in ErrorHandler")
        holder.enqueue(error)
    }
}

@Suppress("unused")
@Module
@InstallIn(ActivityRetainedComponent::class)
interface ErrorHandlerModule {
    @Binds
    fun bind(impl: ErrorHandlerImpl): ErrorHandler
}
