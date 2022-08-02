@file:Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")

package jp.seo.station.ekisagasu.repository

import android.location.Location
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
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
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

@ExperimentalCoroutinesApi
class SearchRepositoryImplTest {

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
        val line = data.lines[0]

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

    @Test
    fun `近傍駅の探索(k=1)`() = runTest {
        // prepare
        val nearStationList = mutableListOf<NearStation?>()
        val nearStationsList = mutableListOf<List<NearStation>>()
        val detectList = mutableListOf<NearStation?>()
        val job = launch(defaultDispatcher) {
            launch { repository.nearestStation.toList(nearStationList) }
            launch { repository.nearestStations.toList(nearStationsList) }
            launch { repository.detectedStation.toList(detectList) }
        }

        every { dataRepository.dataInitialized } returns true
        val codesSlot = slot<List<Int>>()
        coEvery { dataRepository.getLines(capture(codesSlot)) } answers {
            val codes = codesSlot.captured
            codes.map { code ->
                data.lines.find { it.code == code } ?: throw NoSuchElementException()
            }
        }
        val station = data.stations.random()
        coEvery { search.search(any(), any(), 1, 0.0, false) } returns
                SearchResult(0.0, 0.0, 1, 0.0, listOf(station))

        // test
        repository.setSearchK(1)
        val time = Date()
        val location1 = mockk<Location>().also {
            every { it.latitude } returns station.lat + 0.001
            every { it.longitude } returns station.lng
            every { it.time } returns time.time
        }
        val location2 = mockk<Location>().also {
            every { it.latitude } returns station.lat - 0.00001
            every { it.longitude } returns station.lng
            every { it.time } returns time.time
        }
        repository.updateNearestStations(location1)
        repository.updateNearestStations(location2)
        repository.onStopSearch()

        // verify
        // 近傍駅情報の変化
        assertThat(nearStationList.size).isEqualTo(4)
        assertThat(nearStationList[0]).isNull()
        nearStationList[1].also {
            assertThat(it).isNotNull()
            assertThat(it?.station).isEqualTo(station)
            assertThat(it?.time).isEqualTo(time)
            assertThat(it?.distance).isEqualTo(measureDistance(station, location1))
            val lines = station.lines.map { code ->
                data.lines.find { line -> line.code == code }
            }
            assertThat(it?.lines).isEqualTo(lines)
        }
        nearStationList[2].also {
            assertThat(it).isNotNull()
            assertThat(it?.station).isEqualTo(station)
            assertThat(it?.time).isEqualTo(time)
        }
        assertThat(nearStationList[3]).isNull()

        // 近傍駅リストの変化
        assertThat(nearStationsList.size).isEqualTo(4)
        assertThat(nearStationsList[0]).isEmpty()
        nearStationsList[1].also {
            assertThat(it.size).isEqualTo(1)
            assertThat(it[0].station).isEqualTo(station)
        }
        nearStationsList[2].also {
            assertThat(it.size).isEqualTo(1)
            assertThat(it[0].station).isEqualTo(station)
        }
        assertThat(nearStationsList[3]).isEmpty()

        // 近傍駅の変化
        assertThat(detectList.size).isEqualTo(3)
        assertThat(detectList[0]).isNull()
        detectList[1].also {
            assertThat(it).isNotNull()
            assertThat(it?.station).isEqualTo(station)
            assertThat(it?.time).isEqualTo(time)
            assertThat(it?.distance).isEqualTo(measureDistance(station, location1))
        }
        assertThat(detectList[2]).isNull()

        job.cancel()
    }
}
