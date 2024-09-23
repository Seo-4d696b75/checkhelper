package com.seo4d696b75.android.ekisagasu.domain.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

fun <T, R> Flow<T>.mapWithPrevious(
    initialValue: R,
    transform: (value: T, previous: R) -> R,
): Flow<R> {
    var previous: R = initialValue
    return map { value ->
        transform(value, previous).also {
            previous = it
        }
    }
}

fun <T> Flow<T>.onEachWithPrevious(
    initialValue: T,
    action: suspend (value: T, previous: T) -> Unit,
): Flow<T> {
    var previous: T = initialValue
    return onEach { value ->
        action(value, previous)
        previous = value
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R> Flow<T>.mapLatestWithPrevious(
    initialValue: R,
    transform: suspend (value: T, previous: R) -> R,
): Flow<R> {
    var previous: R = initialValue
    return mapLatest { value ->
        transform(value, previous).also {
            previous = it
        }
    }
}

fun <T, R> StateFlow<T>.mapStateIn(
    scope: CoroutineScope,
    convert: (T) -> R,
) = map { convert(it) }.stateIn(
    scope,
    SharingStarted.WhileSubscribed(),
    convert(value),
)

fun <T, R> Flow<T>.mapStateIn(
    scope: CoroutineScope,
    initialValue: R,
    convert: (T) -> R,
) = map { convert(it) }.stateIn(
    scope,
    SharingStarted.WhileSubscribed(),
    initialValue,
)

/**
 * [mapLatest]と同様に元のflowがemitする最新の値を[transform]で変換した結果を流す
 *
 * ただし[transform]実行中に元のflowが次の値をemitした場合、実行中の[transform]が
 * 完了するまで待機し、さらに次の値がemitされた場合は待機をキャンセルする
 */
fun <T, R> Flow<T>.mapLatestBySkip(
    transform: suspend (value: T) -> R,
): Flow<R> {
    val upstream = this
    var runningTransform: Job? = null
    return channelFlow {
        val scope = this
        upstream.collectLatest { value ->
            runningTransform?.join()
            runningTransform = scope.launch {
                val result = transform(value)
                send(result)
            }
        }
    }
}

fun <T, R> Flow<T>.mapLatestBySkip(
    initialValue: R,
    transform: suspend (value: T, previous: R) -> R,
): Flow<R> {
    var previous: R = initialValue
    return mapLatestBySkip { value ->
        transform(value, previous).also {
            previous = it
        }
    }
}
