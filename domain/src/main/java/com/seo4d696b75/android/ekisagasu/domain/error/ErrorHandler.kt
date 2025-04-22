package com.seo4d696b75.android.ekisagasu.domain.error

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 共通エラーハンドリングを抽象化
 */
interface ErrorHandler {
    /**
     * エラーを考慮してコルーチンを起動する
     *
     * @param block ブロック内で発生した例外を補足して[enqueueThrowable]に追加する
     */
    fun CoroutineScope.launchCatching(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit,
    ): Job

    /**
     * エラーを考慮して[StateFlow]に変換する
     *
     * 元の[Flow]で例外が発生すると[enqueueThrowable]に追加し、
     * 適切に消費されたら購読を再開する
     */
    fun <T> Flow<T>.stateInCatching(
        scope: CoroutineScope,
        started: SharingStarted,
        initialValue: T,
    ): StateFlow<T>

    /**
     * 共通機構にエラーを追加する
     *
     * 追加されたエラーは適切な方法でUI表示されるはず
     */
    fun enqueueThrowable(error: Throwable)
}
