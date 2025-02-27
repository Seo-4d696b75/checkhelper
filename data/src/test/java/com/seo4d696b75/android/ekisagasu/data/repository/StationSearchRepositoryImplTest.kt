@file:Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")

package com.seo4d696b75.android.ekisagasu.data.repository

import com.google.common.truth.Truth.assertThat
import com.seo4d696b75.android.ekisagasu.data.fakeLines
import com.seo4d696b75.android.ekisagasu.data.fakeStations
import com.seo4d696b75.android.ekisagasu.data.kdtree.measureDistance
import com.seo4d696b75.android.ekisagasu.data.kdtree.measureSphere
import com.seo4d696b75.android.ekisagasu.data.search.StationSearchRepositoryImpl
import com.seo4d696b75.android.ekisagasu.data.station.LineResponse
import com.seo4d696b75.android.ekisagasu.data.toModel
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.error.ErrorHandler
import com.seo4d696b75.android.ekisagasu.domain.kdtree.NearestSearch
import com.seo4d696b75.android.ekisagasu.domain.kdtree.SearchResult
import com.seo4d696b75.android.ekisagasu.domain.location.Location
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.location.LocationState
import com.seo4d696b75.android.ekisagasu.domain.log.LogCollector
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchState
import com.seo4d696b75.android.ekisagasu.domain.user.UserSetting
import com.seo4d696b75.android.ekisagasu.domain.user.UserSettingRepository
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Date
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
@RunWith(Parameterized::class)
open class StationSearchRepositoryImplTest(private val k: Int) {
    private val dataRepository = mockk<DataRepository>()
    private val search = mockk<NearestSearch>()
    private val locationRepository = mockk<LocationRepository>(relaxUnitFun = true)
    private val settingRepository = mockk<UserSettingRepository>(relaxUnitFun = true)
    private val logger = mockk<LogCollector>(relaxUnitFun = true)
    private val errorHandler = object : ErrorHandler {
        override fun CoroutineScope.launchCatching(
            context: CoroutineContext,
            start: CoroutineStart,
            block: suspend CoroutineScope.() -> Unit
        ) = launch(context, start, block)

        override fun <T> Flow<T>.stateInCatching(
            scope: CoroutineScope,
            started: SharingStarted,
            initialValue: T
        ) = stateIn(scope, started, initialValue)

        override fun enqueueThrowable(error: Throwable) {}
    }

    private val locationState = MutableStateFlow<LocationState>(LocationState.Initializing(1))
    private val userSetting = mockk<UserSetting>()

    private val lines by fakeLines
    private val stations by fakeStations

    @Before
    fun setup() {
        val codesSlot = slot<List<Int>>()
        coEvery { dataRepository.getLines(capture(codesSlot)) } answers {
            val codes = codesSlot.captured
            codes.map { code ->
                lines.find { it.code == code }?.let(LineResponse::toModel) ?: throw NoSuchElementException()
            }
        }

        // Android SDK使ってる関数はモック必要
        mockkStatic(::measureDistance)
        val lat1Slot = slot<Double>()
        val lng1Slot = slot<Double>()
        val lat2Slot = slot<Double>()
        val lng2Slot = slot<Double>()
        every { measureDistance(capture(lat1Slot), capture(lng1Slot), capture(lat2Slot), capture(lng2Slot)) } answers {
            val lat1 = lat1Slot.captured
            val lng1 = lng1Slot.captured
            val lat2 = lat2Slot.captured
            val lng2 = lng2Slot.captured
            measureSphere(lat1, lng1, lat2, lng2).toFloat()
        }

        every { locationRepository.currentLocation } returns locationState
        every { settingRepository.setting } returns flowOf(userSetting)
        every { userSetting.searchK } returns k
    }

    private fun repository(scope: CoroutineScope) = StationSearchRepositoryImpl(
        dataRepository,
        locationRepository,
        settingRepository,
        search,
        logger,
        scope,
        errorHandler,
    )

    @Test
    fun `Line選択`() = runTest {
        // prepare
        val stateInJob = SupervisorJob()
        val repository = repository(this + stateInJob)
        val lineList = mutableListOf<Line?>()
        val stateList = mutableListOf<StationSearchState>()
        val collectJob = launch {
            launch { repository.selectedLine.toList(lineList) }
            launch { repository.state.toList(stateList) }
        }
        advanceUntilIdle()

        val line = lines.random().toModel()

        // test
        repository.selectLine(line)
        locationState.update { LocationState.Idle }
        advanceUntilIdle()
        collectJob.cancelAndJoin()
        stateInJob.cancelAndJoin()

        // verify
        assertThat(lineList).containsExactly(
            null,
            line,
            null,
        ).inOrder()
        assertThat(stateList.last()).isInstanceOf(StationSearchState.Idle::class.java)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun params() = arrayOf(1, 2, 3)
    }

    @Test
    fun `近傍駅の探索`() = runTest {
        // prepare
        val stateInJob = SupervisorJob()
        val repository = repository(this + stateInJob)
        val resultList = mutableListOf<StationSearchState>()
        val collectJob = launch {
            repository.state.toList(resultList)
        }
        advanceUntilIdle()

        val stations = List(k) { stations.random().toModel() }

        coEvery { search.search(any(), any(), k, 0.0, false) } returns
            SearchResult(
                0.0,
                0.0,
                k,
                0.0,
                stations,
            )

        // test

        val time = Date()
        val nearest = stations[0]
        val location1 = Location(
            lat = nearest.lat + 0.001,
            lng = nearest.lng,
            timestamp = time.time,
            elapsedRealtimeMillis = 0L,
        )
        val location2 = Location(
            lat = nearest.lat - 0.00001,
            lng = nearest.lng,
            timestamp = time.time,
            elapsedRealtimeMillis = 0L,
        )
        locationState.update { LocationState.Result(location1, 1) }
        advanceUntilIdle()

        locationState.update { LocationState.Result(location2, 1) }
        advanceUntilIdle()

        locationState.update { LocationState.Idle }
        advanceUntilIdle()

        // verify
        // 近傍駅情報の変化
        assertThat(resultList.size).isEqualTo(5)
        assertThat(resultList[0]).isInstanceOf(StationSearchState.Idle::class.java)
        assertThat(resultList[1]).isInstanceOf(StationSearchState.Initializing::class.java)
        resultList[2].also { r ->
            // 近傍駅情報
            assertThat(r).isInstanceOf(StationSearchState.Result::class.java)
            r as StationSearchState.Result
            assertThat(r.nearest.station).isEqualTo(nearest)
            assertThat(r.nearest.time).isEqualTo(time)
            assertThat(r.nearest.distance).isEqualTo(nearest.measureDistance(location1.lat, location1.lng))

            // 近傍駅リスト
            assertThat(r.nears.size).isEqualTo(stations.size)
            assertThat(r.nears.map { it.station }).isEqualTo(stations)

            // 近傍駅の変化
            assertThat(r.detected.station).isEqualTo(nearest)
            assertThat(r.detected.distance).isEqualTo(nearest.measureDistance(location1.lat, location1.lng))
        }
        resultList[3].also { r ->
            // 近傍駅情報
            assertThat(r).isInstanceOf(StationSearchState.Result::class.java)
            r as StationSearchState.Result
            assertThat(r.nearest.station).isEqualTo(nearest)
            assertThat(r.nearest.time).isEqualTo(time)
            assertThat(r.nearest.distance).isEqualTo(nearest.measureDistance(location2.lat, location2.lng))

            // 近傍駅リスト
            assertThat(r.nears.size).isEqualTo(stations.size)
            assertThat(r.nears.map { it.station }).isEqualTo(stations)

            // 近傍駅の変化
            assertThat(r.detected.station).isEqualTo(nearest)
            assertThat(r.detected.distance).isEqualTo(nearest.measureDistance(location1.lat, location1.lng))
        }
        assertThat(resultList[4]).isInstanceOf(StationSearchState.Idle::class.java)

        coVerifyOrder {
            search.search(nearest.lat + 0.001, nearest.lng, k, 0.0, false)
            search.search(nearest.lat - 0.00001, nearest.lng, k, 0.0, false)
        }

        collectJob.cancelAndJoin()
        stateInJob.cancelAndJoin()
    }
}
