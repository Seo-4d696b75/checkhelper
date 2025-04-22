@file:Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")

package com.seo4d696b75.android.ekisagasu.data.repository

import com.google.common.truth.Truth.assertThat
import com.seo4d696b75.android.ekisagasu.data.database.station.DataVersionEntity
import com.seo4d696b75.android.ekisagasu.data.database.station.RootStationNodeEntity
import com.seo4d696b75.android.ekisagasu.data.database.station.StationDao
import com.seo4d696b75.android.ekisagasu.data.database.station.StationNodeEntity
import com.seo4d696b75.android.ekisagasu.data.fakeData
import com.seo4d696b75.android.ekisagasu.data.fakeLines
import com.seo4d696b75.android.ekisagasu.data.fakeStations
import com.seo4d696b75.android.ekisagasu.data.fakeTree
import com.seo4d696b75.android.ekisagasu.data.file.unzip
import com.seo4d696b75.android.ekisagasu.data.station.DataRepositoryImpl
import com.seo4d696b75.android.ekisagasu.data.toModel
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataVersion
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataVersionState
import com.seo4d696b75.android.ekisagasu.domain.dataset.LatestDataVersion
import com.seo4d696b75.android.ekisagasu.domain.dataset.Prefecture
import com.seo4d696b75.android.ekisagasu.domain.dataset.PrefectureRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
import java.util.Date

@ExperimentalCoroutinesApi
class DataRepositoryImplTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val dao = mockk<StationDao>()
    private val prefectureRepository = mockk<PrefectureRepository>()
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var repository: DataRepository

    private val stations by fakeStations
    private val lines by fakeLines
    private val tree by fakeTree

    @Before
    fun setup() {
        repository = DataRepositoryImpl(dao, json, prefectureRepository)

        val slop = slot<Int>()
        every { prefectureRepository[capture(slop)] } answers {
            Prefecture(slop.captured, "name")
        }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private val info = LatestDataVersion(
        version = 1,
        length = 1024,
    )

    @Test
    fun `データが初期化前`() = runTest {
        // before update (no data)
        every { dao.getCurrentDataVersion() } returns flowOf(null)
        val state = repository.dataVersion.first()
        assertThat(state).isEqualTo(DataVersionState.None)

        // after update
        val version = DataVersionEntity(info.version)
        every { dao.getCurrentDataVersion() } returns flowOf(version)
        val state1 = repository.dataVersion.first()
        assertThat(state1).isInstanceOf(DataVersionState.Initialized::class.java)
    }

    @Test
    fun `データのアップデート - 失敗`() = runTest {
        // update
        val result = runCatching {
            repository.updateData(info, tempFolder.newFolder())
        }
        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)

        coVerify(exactly = 0) {
            dao.updateData(any(), any(), any(), any())
        }
    }

    @Test
    fun `データのアップデート - 成功`() = runTest {
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
        coEvery { dao.updateData(info.version, any(), any(), any()) } returns DataVersion(info.version, Date())
        val result = repository.updateData(info, dir)
        assertThat(result.version).isEqualTo(info.version)

        // verify data version flow
        coVerify(exactly = 1) {
            dao.updateData(info.version, any(), any(), any())
        }
    }

    @Test
    fun `Daoの呼び出し`() = runTest {
        // prepare data

        // mock dao operation
        val stationCodeSlot = slot<Int>()
        coEvery { dao.getStation(capture(stationCodeSlot)) } answers {
            val code = stationCodeSlot.captured
            stations.find { it.code == code }?.toEntity() ?: throw NoSuchElementException()
        }
        val lineCodeSlot = slot<Int>()
        coEvery { dao.getLine(capture(lineCodeSlot)) } answers {
            val code = lineCodeSlot.captured
            lines.find { it.code == code }?.toEntity() ?: throw NoSuchElementException()
        }
        val listCodesSlot = slot<List<Int>>()
        coEvery { dao.getLines(capture(listCodesSlot)) } answers {
            listCodesSlot
                .captured
                .map { code ->
                    lines.find { it.code == code }?.toEntity() ?: throw NoSuchElementException()
                }
        }
        coEvery { dao.getRootStationNode() }.answers { RootStationNodeEntity(tree.root) }
        coEvery { dao.getStationNodes() } answers {
            tree.nodes.map { StationNodeEntity.fromModel(it) }
        }
        coEvery { dao.getDataVersionHistory() } answers {
            listOf(
                DataVersionEntity(0),
                DataVersionEntity(1),
            )
        }

        // test
        val station = stations.random().toModel()
        assertThat(repository.getStation(station.code)).isEqualTo(station)
        val line = lines.random().toModel()
        assertThat(repository.getLine(line.code)).isEqualTo(line)
        val history = repository.getDataVersionHistory()
        assertThat(history.size).isEqualTo(2)
        val root = repository.getStationKdTree().root
        assertThat(root).isEqualTo(tree.root)

        // verify
        coVerifyOrder {
            dao.getStation(station.code)
            dao.getLines(station.lines.map { it.code })
            dao.getLine(line.code)
            dao.getDataVersionHistory()
            dao.getRootStationNode()
            dao.getStationNodes()
        }
        confirmVerified(dao)
    }
}
