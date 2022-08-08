@file:Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")

package jp.seo.station.ekisagasu.repository

import android.location.Location
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jp.seo.station.ekisagasu.fakeData
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.model.NearStation
import jp.seo.station.ekisagasu.repository.impl.SearchRepositoryImpl
import jp.seo.station.ekisagasu.search.NearestSearch
import jp.seo.station.ekisagasu.search.SearchResult
import jp.seo.station.ekisagasu.search.measureDistance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*

@ExperimentalCoroutinesApi
@RunWith(Parameterized::class)
open class SearchRepositoryImplTest(
    private val k: Int,
) {

    private val dataRepository = mockk<DataRepository>()
    private val search = mockk<NearestSearch>()

    private val repository: SearchRepository = SearchRepositoryImpl(
        dataRepository, search
    )

    private val defaultDispatcher = UnconfinedTestDispatcher()

    private val data by fakeData

    @Before
    fun setup() {
        Dispatchers.setMain(defaultDispatcher)

        every { dataRepository.dataInitialized } returns true

        val codesSlot = slot<List<Int>>()
        coEvery { dataRepository.getLines(capture(codesSlot)) } answers {
            val codes = codesSlot.captured
            codes.map { code ->
                data.lines.find { it.code == code } ?: throw NoSuchElementException()
            }
        }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Line選択`() = runTest {
        // prepare
        val lineList = mutableListOf<Line?>()
        val job = launch(defaultDispatcher) {
            repository.selectedLine.toList(lineList)
        }
        val line = data.lines.random()

        // test
        repository.selectLine(line)
        repository.onStopSearch()
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
        val nearStationList = mutableListOf<NearStation?>()
        val nearStationsList = mutableListOf<List<NearStation>>()
        val detectList = mutableListOf<NearStation?>()
        val job = launch(defaultDispatcher) {
            launch { repository.nearestStation.toList(nearStationList) }
            launch { repository.nearestStations.toList(nearStationsList) }
            launch { repository.detectedStation.toList(detectList) }
        }

        val stations = List(k) { data.stations.random() }

        coEvery { search.search(any(), any(), k, 0.0, false) } returns SearchResult(
            0.0,
            0.0,
            k,
            0.0,
            stations
        )

        // test
        repository.setSearchK(k)
        val time = Date()
        val nearest = stations[0]
        val location1 = mockk<Location>().also {
            every { it.latitude } returns nearest.lat + 0.001
            every { it.longitude } returns nearest.lng
            every { it.time } returns time.time
        }
        val location2 = mockk<Location>().also {
            every { it.latitude } returns nearest.lat - 0.00001
            every { it.longitude } returns nearest.lng
            every { it.time } returns time.time
        }
        repository.updateNearestStations(location1)
        repository.updateNearestStations(location2)
        repository.onStopSearch()

        advanceUntilIdle()

        // verify
        // 近傍駅情報の変化
        assertThat(nearStationList.size).isEqualTo(4)
        assertThat(nearStationList[0]).isNull()
        nearStationList[1].also {
            assertThat(it).isNotNull()
            assertThat(it?.station).isEqualTo(nearest)
            assertThat(it?.time).isEqualTo(time)
            assertThat(it?.distance).isEqualTo(measureDistance(nearest, location1))
            val lines = nearest.lines.map { code ->
                data.lines.find { line -> line.code == code }
            }
            assertThat(it?.lines).isEqualTo(lines)
        }
        nearStationList[2].also {
            assertThat(it).isNotNull()
            assertThat(it?.station).isEqualTo(nearest)
            assertThat(it?.time).isEqualTo(time)
        }
        assertThat(nearStationList[3]).isNull()

        // 近傍駅リストの変化
        assertThat(nearStationsList.size).isEqualTo(4)
        assertThat(nearStationsList[0]).isEmpty()
        nearStationsList[1].also {
            assertThat(it.size).isEqualTo(stations.size)
            assertThat(it.map { it.station }).isEqualTo(stations)
        }
        nearStationsList[2].also {
            assertThat(it.size).isEqualTo(stations.size)
            assertThat(it.map { it.station }).isEqualTo(stations)
        }
        assertThat(nearStationsList[3]).isEmpty()

        // 近傍駅の変化
        assertThat(detectList.size).isEqualTo(3)
        assertThat(detectList[0]).isNull()
        detectList[1].also {
            assertThat(it).isNotNull()
            assertThat(it?.station).isEqualTo(nearest)
            assertThat(it?.time).isEqualTo(time)
            assertThat(it?.distance).isEqualTo(measureDistance(nearest, location1))
        }
        assertThat(detectList[2]).isNull()

        coVerifyOrder {
            search.search(nearest.lat + 0.001, nearest.lng, k, 0.0, false)
            search.search(nearest.lat - 0.00001, nearest.lng, k, 0.0, false)
        }

        job.cancel()
    }
}
