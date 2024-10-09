@file:Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")

package com.seo4d696b75.android.ekisagasu.data.repository

import com.google.common.truth.Truth.assertThat
import com.seo4d696b75.android.ekisagasu.data.fakeLines
import com.seo4d696b75.android.ekisagasu.data.fakeStations
import com.seo4d696b75.android.ekisagasu.data.kdtree.measureDistance
import com.seo4d696b75.android.ekisagasu.data.kdtree.measureSphere
import com.seo4d696b75.android.ekisagasu.data.search.StationSearchRepositoryImpl
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.kdtree.NearestSearch
import com.seo4d696b75.android.ekisagasu.domain.kdtree.SearchResult
import com.seo4d696b75.android.ekisagasu.domain.location.Location
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.log.LogCollector
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchResult
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
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
    private val logger = mockk<LogCollector>(relaxUnitFun = true)
    private val job = SupervisorJob()
    private val defaultDispatcher = UnconfinedTestDispatcher()
    private val scope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = job + defaultDispatcher
    }


    private val currentLocation = MutableStateFlow<Location?>(null)
    private val isRunning = MutableStateFlow(true)

    private lateinit var repository: StationSearchRepository

    private val lines by fakeLines
    private val stations by fakeStations

    @Before
    fun setup() {
        // Dispatchers.setMain(defaultDispatcher)

        every { dataRepository.dataInitialized } returns true

        val codesSlot = slot<List<Int>>()
        coEvery { dataRepository.getLines(capture(codesSlot)) } answers {
            val codes = codesSlot.captured
            codes.map { code ->
                lines.find { it.code == code } ?: throw NoSuchElementException()
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

        every { locationRepository.isRunning } returns isRunning
        every { locationRepository.currentLocation } returns currentLocation
        repository = StationSearchRepositoryImpl(
            dataRepository,
            locationRepository,
            search,
            logger,
            scope,
        )
    }

    @After
    fun teardown() {
        // Dispatchers.resetMain()
        job.cancel()
    }

    @Test
    fun `Line選択`() = runTest {
        // prepare
        val lineList = mutableListOf<Line?>()
        val job = launch(defaultDispatcher) {
            launch { repository.selectedLine.toList(lineList) }
            launch { repository.result.collect() }
        }
        val line = lines.random()

        // test
        repository.selectLine(line)
        isRunning.update { false }
        advanceUntilIdle()

        // verify
        assertThat(lineList).containsExactly(
            null,
            line,
            null,
        ).inOrder()
        job.cancel()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun params() = arrayOf(1, 2, 3)
    }

    @Test
    fun `近傍駅の探索`() = runTest {
        // prepare
        val resultList = mutableListOf<StationSearchResult?>()
        val job = launch(defaultDispatcher) {
            repository.result.toList(resultList)
        }

        val stations = List(k) { stations.random() }

        coEvery { search.search(any(), any(), k, 0.0, false) } returns
            SearchResult(
                0.0,
                0.0,
                k,
                0.0,
                stations,
            )

        // test
        repository.setSearchK(k)
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
        currentLocation.update { location1 }
        advanceUntilIdle()

        currentLocation.update { location2 }
        advanceUntilIdle()

        isRunning.update { false }
        currentLocation.update { null }
        advanceUntilIdle()

        // verify
        // 近傍駅情報の変化
        assertThat(resultList.size).isEqualTo(4)
        assertThat(resultList.first()).isNull()
        resultList[1].also { r ->
            // 近傍駅情報
            assertThat(r).isNotNull()
            assertThat(r?.nearest?.station).isEqualTo(nearest)
            assertThat(r?.nearest?.time).isEqualTo(time)
            assertThat(r?.nearest?.distance).isEqualTo(nearest.measureDistance(location1.lat, location1.lng))
            val lines = nearest.lines.map { code ->
                lines.find { line -> line.code == code }
            }
            assertThat(r?.nearest?.lines).isEqualTo(lines)

            // 近傍駅リスト
            assertThat(r?.nears?.size).isEqualTo(stations.size)
            assertThat(r?.nears?.map { it.station }).isEqualTo(stations)

            // 近傍駅の変化
            assertThat(r?.detected?.station).isEqualTo(nearest)
            assertThat(r?.detected?.distance).isEqualTo(nearest.measureDistance(location1.lat, location1.lng))
        }
        resultList[2].also { r ->
            // 近傍駅情報
            assertThat(r).isNotNull()
            assertThat(r?.nearest?.station).isEqualTo(nearest)
            assertThat(r?.nearest?.time).isEqualTo(time)
            assertThat(r?.nearest?.distance).isEqualTo(nearest.measureDistance(location2.lat, location2.lng))

            // 近傍駅リスト
            assertThat(r?.nears?.size).isEqualTo(stations.size)
            assertThat(r?.nears?.map { it.station }).isEqualTo(stations)

            // 近傍駅の変化
            assertThat(r?.detected?.station).isEqualTo(nearest)
            assertThat(r?.detected?.distance).isEqualTo(nearest.measureDistance(location1.lat, location1.lng))
        }
        assertThat(resultList[3]).isNull()

        coVerifyOrder {
            search.search(nearest.lat + 0.001, nearest.lng, k, 0.0, false)
            search.search(nearest.lat - 0.00001, nearest.lng, k, 0.0, false)
        }

        job.cancel()
    }
}
