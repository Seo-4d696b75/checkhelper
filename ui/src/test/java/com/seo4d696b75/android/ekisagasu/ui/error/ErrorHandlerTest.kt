package com.seo4d696b75.android.ekisagasu.ui.error

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ErrorHandlerTest {

    private lateinit var handler: ErrorHandler
    private lateinit var holder: ErrorStateHolder

    @Before
    fun setup() {
        holder = ErrorStateHolder()
        handler = ErrorHandlerImpl(holder)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testLaunchCatching() = runTest {
        with(handler) {
            val stateList = mutableListOf<ErrorState>()
            launch {
                holder.errorState.toList(stateList)
            }
            launchCatching {
                delay(100)
                throw RuntimeException()
            }
            advanceUntilIdle()

            assertThat(stateList.size).isEqualTo(2)
            assertThat(stateList[0]).isInstanceOf(ErrorState.Empty::class.java)
            val state = stateList[1]
            assertThat(state).isInstanceOf(ErrorState.Queued::class.java)
            state as ErrorState.Queued
            assertThat(state.consumed).isFalse()
            assertThat(state.error).isInstanceOf(RuntimeException::class.java)
        }
    }

    @Test
    fun testStateInCatching() = runTest {
        with(handler) {
            val upstream = flow {
                emit(1)
                delay(100)
                emit(2)
                delay(100)
                throw RuntimeException()
            }
            val stateInJob = SupervisorJob()
            val stateFlow = upstream.stateInCatching(
                this@runTest + stateInJob,
                SharingStarted.WhileSubscribed(),
                0
            )

            val values = mutableListOf<Int>()
            val collectJob = launch {
                stateFlow.toList(values)
            }

            // wait until error reported
            val awaitNextError: suspend () -> Throwable = {
                holder
                    .errorState
                    .map { it.errorToBeShown }
                    .filterNotNull()
                    .first()
            }

            val e1 = awaitNextError()
            assertThat(e1).isInstanceOf(RuntimeException::class.java)
            assertThat(values).isEqualTo(listOf(0, 1, 2))

            holder.consume()

            val e2 = awaitNextError()
            assertThat(e2).isInstanceOf(RuntimeException::class.java)
            assertThat(values).isEqualTo(listOf(0, 1, 2, 1, 2))

            stateInJob.cancelAndJoin()
            collectJob.cancelAndJoin()
        }
    }
}
