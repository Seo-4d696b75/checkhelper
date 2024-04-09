@file:Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")

package jp.seo.station.ekisagasu.repository

import com.google.common.truth.Truth.assertThat
import com.seo4d696b75.android.ekisagasu.data.database.DataVersion
import com.seo4d696b75.android.ekisagasu.data.database.StationDao
import com.seo4d696b75.android.ekisagasu.data.kdtree.RootStationNode
import com.seo4d696b75.android.ekisagasu.data.station.DataRepository
import com.seo4d696b75.android.ekisagasu.data.station.DataRepositoryImpl
import com.seo4d696b75.android.ekisagasu.data.station.LatestDataVersion
import com.seo4d696b75.android.ekisagasu.data.utils.unzip
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot
import jp.seo.station.ekisagasu.fakeData
import jp.seo.station.ekisagasu.fakeLines
import jp.seo.station.ekisagasu.fakeStations
import jp.seo.station.ekisagasu.fakeTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

@ExperimentalCoroutinesApi
class DataRepositoryImplTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val dao = mockk<StationDao>()
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var repository: DataRepository

    private val stations by fakeStations
    private val lines by fakeLines
    private val tree by fakeTree

    @Before
    fun setup() {
        repository = DataRepositoryImpl(dao, json)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private val info =
        LatestDataVersion(
            version = 1,
            length = 1024,
        )

    @Test
    fun `データが初期化前`() =
        runTest {
            // before update (no data)
            coEvery { dao.getCurrentDataVersion() } returns null
            repository.getDataVersion()
            assertThat(repository.dataInitialized).isFalse()

            // after update
            val version = DataVersion(info.version)
            coEvery { dao.getCurrentDataVersion() } returns version
            repository.getDataVersion()
            assertThat(repository.dataInitialized).isTrue()
        }

    @Test
    fun `データのアップデート - 失敗`() =
        runTest {
            // watch flow
            val dataVersionList = mutableListOf<DataVersion?>()
            val job =
                launch {
                    repository.dataVersion.toList(dataVersionList)
                }

            // update
            val result =
                runCatching {
                    repository.updateData(info, tempFolder.newFolder())
                }
            assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)

            // verify data version flow
            assertThat(dataVersionList.last()).isNull()
            job.cancel()
        }

    @Test
    fun `データのアップデート - 成功`() =
        runTest {
            // watch flow
            val dataVersionList = mutableListOf<DataVersion?>()
            val job =
                launch {
                    repository.dataVersion.toList(dataVersionList)
                }

            val dir = tempFolder.newFolder()
            val zip = File(dir, "json.zip")
            // copy json.zip
            fakeData().use { input ->
                zip.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            // unzip
            unzip(zip, dir)

            // test
            coEvery { dao.updateData(any(), any(), any(), any()) } returns DataVersion(info.version)
            val result = repository.updateData(info, dir)
            assertThat(result.version).isEqualTo(info.version)

            // verify data version flow
            assertThat(dataVersionList.size).isGreaterThan(1)
            assertThat(dataVersionList[0]).isNull()
            assertThat(dataVersionList.last()?.version).isEqualTo(info.version)
            job.cancel()

            coVerify { dao.updateData(info.version, any(), any(), any()) }
        }

    @Test
    fun `Daoの呼び出し`() =
        runTest {
            // prepare data

            // mock dao operation
            val stationCodeSlot = slot<Int>()
            coEvery { dao.getStation(capture(stationCodeSlot)) } answers {
                val code = stationCodeSlot.captured
                stations.find { it.code == code } ?: throw NoSuchElementException()
            }
            val lineCodeSlot = slot<Int>()
            coEvery { dao.getLine(capture(lineCodeSlot)) } answers {
                val code = lineCodeSlot.captured
                lines.find { it.code == code } ?: throw NoSuchElementException()
            }
            coEvery { dao.getRootStationNode() }.answers { RootStationNode(tree.root) }
            coEvery { dao.getStationNodes() } answers { tree.nodes }
            coEvery { dao.getDataVersionHistory() } returns
                listOf(
                    DataVersion(0),
                    DataVersion(1),
                )

            // test
            val station = stations.random()
            assertThat(repository.getStation(station.code)).isEqualTo(station)
            val line = lines.random()
            assertThat(repository.getLine(line.code)).isEqualTo(line)
            val history = repository.getDataVersionHistory()
            assertThat(history.size).isEqualTo(2)
            val root = repository.getStationKdTree().root
            assertThat(root).isEqualTo(tree.root)

            // verify
            coVerifyOrder {
                dao.getStation(station.code)
                dao.getLine(line.code)
                dao.getDataVersionHistory()
                dao.getRootStationNode()
                dao.getStationNodes()
            }
            confirmVerified(dao)
        }
}
