package com.seo4d696b75.android.ekisagasu.data.cache

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryCacheTest {

    private val store = MemoryCacheStore()

    private var counter = 0
    private val fetch = mockk<suspend () -> Int>().also {
        coEvery { it.invoke() }.coAnswers {
            delay(100)
            counter++
        }
    }

    private val cache = store.cacheOf(null, fetch)

    @Test
    fun testMemoryCacheStore_get() {
        val flow1: MutableStateFlow<CacheState<String>> = store["key"]
        val flow2: MutableStateFlow<CacheState<String>> = store["key"]
        val flow3: MutableStateFlow<CacheState<String>> = store["key2"]
        val flow4: MutableStateFlow<CacheState<Int>> = store["key"]

        assertThat(flow1).isSameInstanceAs(flow2)
        assertThat(flow1).isNotSameInstanceAs(flow3)
        assertThat(flow1).isNotSameInstanceAs(flow4)
    }

    @Test
    fun testMemoryCache_invalidate() = runTest {
        val collector1: FlowCollector<Int?> = mockk(relaxed = true)
        val collector2: FlowCollector<Int?> = mockk(relaxed = true)
        val collectJob = launch {
            launch {
                cache().collect(collector1)
            }
            launch {
                cache().collect(collector2)
            }
        }

        // wait until initialization completed
        advanceUntilIdle()
        coVerifyOrder {
            collector1.emit(null)
            collector1.emit(0)
        }
        coVerifyOrder {
            collector2.emit(null)
            collector2.emit(0)
        }
        coVerify(exactly = 0) {
            collector1.emit(1)
            collector2.emit(1)
        }
        coVerify(exactly = 1) {
            fetch.invoke()
        }

        // invalidate
        cache.invalidate()
        advanceUntilIdle()
        coVerifyOrder {
            collector1.emit(null)
            collector1.emit(0)
            collector1.emit(1)
        }
        coVerifyOrder {
            collector2.emit(null)
            collector2.emit(0)
            collector2.emit(1)
        }
        coVerify(exactly = 2) {
            fetch.invoke()
        }

        collectJob.cancelAndJoin()
    }

    @Test
    fun testMemoryCache_invalidate_whileInitialization() = runTest {
        val initializationJob = Job()
        coEvery { fetch.invoke() }.coAnswers {
            initializationJob.join()
            counter++
        }

        val collector: FlowCollector<Int?> = mockk(relaxed = true)
        val collectJob = launch {
            cache().collect(collector)
        }

        advanceUntilIdle()
        coVerify {
            collector.emit(null)
        }
        coVerify(exactly = 0) {
            collector.emit(0)
        }

        // invalidate
        cache.invalidate()
        advanceUntilIdle()
        coVerify(exactly = 0) {
            collector.emit(0)
        }

        // complete initialization
        initializationJob.cancel()
        advanceUntilIdle()
        coVerifyOrder {
            collector.emit(null)
            collector.emit(0)
        }
        coVerify(exactly = 1) {
            fetch.invoke()
        }

        collectJob.cancelAndJoin()
    }

    @Test
    fun testMemoryCache_invalidate_atOneTime() = runTest {
        val collector: FlowCollector<Int?> = mockk(relaxed = true)
        val collectJob = launch {
            cache().collect(collector)
        }

        // wait until initialization completed
        advanceUntilIdle()
        coVerifyOrder {
            collector.emit(null)
            collector.emit(0)
        }

        // invalidate
        val invalidateJob = Job()
        coEvery { fetch.invoke() }.coAnswers {
            invalidateJob.join()
            counter++
        }

        cache.invalidate()
        advanceUntilIdle()

        cache.invalidate()
        advanceUntilIdle()

        coVerify(exactly = 0) {
            collector.emit(1)
        }

        invalidateJob.cancel()
        advanceUntilIdle()

        coVerifyOrder {
            collector.emit(null)
            collector.emit(0)
            collector.emit(1)
        }
        coVerify(exactly = 2) {
            fetch.invoke()
        }

        collectJob.cancelAndJoin()
    }

    @Test
    fun testMemoryCache_invalidate_error() = runTest {
        val collector: FlowCollector<Int?> = mockk(relaxed = true)
        val collectDeferred = async {
            runCatching {
                cache().collect(collector)
            }
        }

        // wait until initialization completed
        advanceUntilIdle()
        coVerifyOrder {
            collector.emit(null)
            collector.emit(0)
        }

        // error on invalidate
        coEvery { fetch.invoke() }.coAnswers {
            delay(100)
            throw RuntimeException()
        }

        cache.invalidate()
        advanceUntilIdle()
        val result = collectDeferred.await()
        assertThat(result.exceptionOrNull()).isInstanceOf(RuntimeException::class.java)

        // restart collect
        coEvery { fetch.invoke() }.coAnswers {
            delay(100)
            counter++
        }
        val restartCollectJob = launch {
            cache().collect(collector)
        }

        advanceUntilIdle()
        coVerifyOrder {
            collector.emit(null)
            collector.emit(0)
            collector.emit(1)
        }

        restartCollectJob.cancelAndJoin()
    }

    @Test
    fun testMemoryCache_refresh() = runTest {
        val collector1: FlowCollector<Int?> = mockk(relaxed = true)
        val collector2: FlowCollector<Int?> = mockk(relaxed = true)
        val collectJob = launch {
            launch {
                cache().collect(collector1)
            }
            launch {
                cache().collect(collector2)
            }
        }

        // wait until initialization completed
        advanceUntilIdle()
        coVerify(exactly = 1) {
            fetch.invoke()
        }

        // refresh
        val result = cache.refresh()
        assertThat(result).isEqualTo(1)
        advanceUntilIdle()
        coVerifyOrder {
            collector1.emit(null)
            collector1.emit(0)
            collector1.emit(1)
        }
        coVerifyOrder {
            collector2.emit(null)
            collector2.emit(0)
            collector2.emit(1)
        }
        coVerify(exactly = 2) {
            fetch.invoke()
        }

        collectJob.cancelAndJoin()
    }

    @Test
    fun testMemoryCache_refresh_atOneTime() = runTest {
        val collector: FlowCollector<Int?> = mockk(relaxed = true)
        val collectJob = launch {
            cache().collect(collector)
        }

        // wait until initialization completed
        advanceUntilIdle()
        coVerifyOrder {
            collector.emit(null)
            collector.emit(0)
        }

        // invalidate
        val invalidateJob = Job()
        coEvery { fetch.invoke() }.coAnswers {
            invalidateJob.join()
            counter++
        }

        cache.invalidate()
        advanceUntilIdle()
        coVerify(exactly = 0) {
            collector.emit(1)
        }

        // refresh
        val refreshDeferred = async {
            cache.refresh()
        }

        invalidateJob.cancel()
        advanceUntilIdle()
        coVerifyOrder {
            collector.emit(null)
            collector.emit(0)
            collector.emit(1)
            collector.emit(2)
        }
        coVerify(exactly = 3) {
            fetch.invoke()
        }

        val refreshResult = refreshDeferred.await()
        assertThat(refreshResult).isEqualTo(2)

        collectJob.cancelAndJoin()
    }

    @Test
    fun testMemoryCache_refresh_error() = runTest {
        val collector: FlowCollector<Int?> = mockk(relaxed = true)
        val collectJob = launch {
            cache().collect(collector)
        }

        // wait until initialization completed
        advanceUntilIdle()
        coVerifyOrder {
            collector.emit(null)
            collector.emit(0)
        }

        // refresh
        coEvery { fetch.invoke() }.coAnswers {
            delay(100)
            throw RuntimeException()
        }

        val result = runCatching {
            cache.refresh()
        }
        advanceUntilIdle()
        coVerifyOrder {
            collector.emit(null)
            collector.emit(0)
        }
        assertThat(result.exceptionOrNull()).isInstanceOf(RuntimeException::class.java)

        collectJob.cancelAndJoin()
    }
}
