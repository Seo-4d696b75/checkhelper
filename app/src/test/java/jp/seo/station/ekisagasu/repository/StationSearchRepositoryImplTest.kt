@file:Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")

package jp.seo.station.ekisagasu.repository

import android.location.Location
import com.google.common.truth.Truth.assertThat
import com.seo4d696b75.android.ekisagasu.domain.kdtree.NearestSearch
import com.seo4d696b75.android.ekisagasu.domain.kdtree.SearchResult
import com.seo4d696b75.android.ekisagasu.data.kdtree.measureDistance
import com.seo4d696b75.android.ekisagasu.data.kdtree.measureSphere
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.data.search.StationSearchRepositoryImpl
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import jp.seo.station.ekisagasu.fakeLines
import jp.seo.station.ekisagasu.fakeStations
import jp.seo.station.ekisagasu.model.NearStation
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
import java.util.Date

@ExperimentalCoroutinesApi
@RunWith(Parameterized::class)
open class StationSearchRepositoryImplTest(private val k: Int,) {
    private val dataRepository = mockk<DataRepository>()
    private val search = mockk<NearestSearch>()

    private val repository: StationSearchRepository =
        StationSearchRepositoryImpl(
            dataRepository,
            search,
        )

    private val defaultDispatcher = UnconfinedTestDispatcher()

    private val lines by fakeLines
    private val stations by fakeStations

    @Before
    fun setup() {
        Dispatchers.setMain(defaultDispatcher)

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
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Line選択`() =
        runTest {
            // prepare
            val lineList = mutableListOf<Line?>()
            val job =
                launch(defaultDispatcher) {
                    repository.selectedLine.toList(lineList)
                }
            val line = lines.random()

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
    fun `近傍駅の探索`() =
        runTest {
            // prepare
            val nearStationList = mutableListOf<NearStation?>()
            val nearStationsList = mutableListOf<List<NearStation>>()
            val detectList = mutableListOf<NearStation?>()
            val job =
                launch(defaultDispatcher) {
                    launch { repository.nearestStation.toList(nearStationList) }
                    launch { repository.nearestStations.toList(nearStationsList) }
                    launch { repository.detectedStation.toList(detectList) }
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
            val location1 =
                mockk<Location>().also {
                    every { it.latitude } returns nearest.lat + 0.001
                    every { it.longitude } returns nearest.lng
                    every { it.time } returns time.time
                }
            val location2 =
                mockk<Location>().also {
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
                assertThat(it?.distance).isEqualTo(nearest.measureDistance(location1))
                val lines =
                    nearest.lines.map { code ->
                        lines.find { line -> line.code == code }
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
                assertThat(it?.distance).isEqualTo(nearest.measureDistance(location1))
            }
            assertThat(detectList[2]).isNull()

            coVerifyOrder {
                search.search(nearest.lat + 0.001, nearest.lng, k, 0.0, false)
                search.search(nearest.lat - 0.00001, nearest.lng, k, 0.0, false)
            }

            job.cancel()
        }
}
