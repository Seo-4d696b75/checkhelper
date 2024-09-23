package com.seo4d696b75.android.ekisagasu.domain

import com.seo4d696b75.android.ekisagasu.domain.coroutine.mapLatestBySkip
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FlowUtilTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun mapLatest() = runTest {
        val mapCollector: (String) -> Unit = mockk(relaxed = true)
        val finalCollector: (String) -> Unit = mockk(relaxed = true)
        flow {
            emit("a")
            delay(100)
            emit("b")
        }.mapLatest {
            mapCollector("Start $it")
            delay(200)
            "Computed $it"
        }.collect(finalCollector)

        verifyOrder {
            mapCollector("Start a")
            mapCollector("Start b")
            finalCollector("Computed b")
        }
        verify(exactly = 1) {
            finalCollector(any())
        }
    }

    @Test
    fun mapLatestBySkip_noSkip() = runTest {
        val mapCollector: (String) -> Unit = mockk(relaxed = true)
        val finalCollector: (String) -> Unit = mockk(relaxed = true)
        flow {
            emit("a")
            delay(100)
            emit("b")
        }.mapLatestBySkip {
            mapCollector("Start $it")
            delay(200)
            "Computed $it"
        }.collect(finalCollector)

        verifyOrder {
            mapCollector("Start a")
            finalCollector("Computed a")
            mapCollector("Start b")
            finalCollector("Computed b")
        }
    }

    @Test
    fun mapLatestBySkip_Skip() = runTest {
        val mapCollector: (String) -> Unit = mockk(relaxed = true)
        val finalCollector: (String) -> Unit = mockk(relaxed = true)
        flow {
            emit("a")
            delay(100)
            emit("b")
            delay(50)
            emit("c")
        }.mapLatestBySkip {
            mapCollector("Start $it")
            delay(200)
            "Computed $it"
        }.collect(finalCollector)

        verifyOrder {
            mapCollector("Start a")
            finalCollector("Computed a")
            mapCollector("Start c")
            finalCollector("Computed c")
        }
        verify(exactly = 2) {
            finalCollector(any())
        }
    }
}
