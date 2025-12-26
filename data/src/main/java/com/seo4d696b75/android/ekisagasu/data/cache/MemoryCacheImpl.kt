package com.seo4d696b75.android.ekisagasu.data.cache

import com.seo4d696b75.android.ekisagasu.domain.cache.MemoryCache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal inline fun <reified T : Any> MemoryCacheStore.cacheOf(
    key: String? = null,
    noinline fetch: suspend () -> T,
): MemoryCache<T> = MemoryCacheImpl(this[key], fetch)

internal class MemoryCacheImpl<T : Any>(
    private val upstream: MutableStateFlow<CacheState<T>>,
    private val fetch: suspend () -> T,
) : MemoryCache<T> {
    override fun invoke(): Flow<T?> = flow {
        upstream.collect { current ->
            emit(current)

            if (current.shouldRefresh) {
                // throw an error to the subscriber if any
                tryFetchWithLock(current)?.getOrThrow()
            }
        }
    }.map {
        it.value
    }.distinctUntilChanged()

    private suspend fun tryFetchWithLock(current: CacheState<T>): Result<T>? {
        val loading = current.copy(isLoading = true)
        // try to lock before calling `fetch`
        // if multiple subscribers, only single coroutine can call it at a time
        val hasLocked = upstream.getAndUpdate {
            if (it == current) {
                loading
            } else {
                it
            }
        } == current
        return if (hasLocked) {
            val result = runCatching { fetch() }
            upstream.update {
                if (it != loading) {
                    throw ConcurrentModificationException()
                }
                result.fold(
                    onSuccess = { value ->
                        CacheState(
                            value = value,
                            isDirty = false,
                            isLoading = false,
                        )
                    },
                    // set `isLoading` back to false so that other subscribers
                    // or restarted collecting can try to call `fetch` again.
                    onFailure = { current },
                )
            }
            result
        } else {
            null
        }
    }

    override fun invalidate() {
        upstream.update {
            if (it.value != null && !it.isDirty && !it.isLoading) {
                it.copy(isDirty = true)
            } else {
                it
            }
        }
    }

    override suspend fun refresh(): T {
        while (true) {
            var current = upstream.value
            if (current.isLoading) {
                // wait until fetch completed in another coroutine
                current = upstream.filter { !it.isLoading }.first()
            }

            // try to fetch by itself
            val result = tryFetchWithLock(current)
            if (result != null) {
                return result.getOrThrow()
            }
        }
    }
}
