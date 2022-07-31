@file:Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")

package jp.seo.station.ekisagasu.repository

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import jp.seo.station.ekisagasu.api.APIClient
import jp.seo.station.ekisagasu.database.DataVersion
import jp.seo.station.ekisagasu.database.StationDao
import jp.seo.station.ekisagasu.model.DataLatestInfo
import jp.seo.station.ekisagasu.model.DataUpdateProgress
import jp.seo.station.ekisagasu.model.StationData
import jp.seo.station.ekisagasu.repository.impl.DataRepositoryImpl
import jp.seo.station.ekisagasu.usecase.DataUpdateResult
import jp.seo.station.ekisagasu.usecase.DataUpdateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader

@ExperimentalCoroutinesApi
@ExperimentalSerializationApi
class DataRepositoryImplTest {

    private val defaultDispatcher = UnconfinedTestDispatcher()
    private val dao = mockk<StationDao>()
    private val api = mockk<APIClient>()
    private val useCase = mockk<DataUpdateUseCase>()
    private val updateProgress = MutableSharedFlow<DataUpdateProgress>()
    private lateinit var repository: DataRepository

    @Before
    fun setup() {
        Dispatchers.setMain(defaultDispatcher)
        every { useCase.progress } returns updateProgress
        repository = DataRepositoryImpl(dao, api, useCase)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private val info = DataLatestInfo(
        version = 1,
        length = 1024,
        url = "https://test.com/data.json"
    )

    @Test
    fun `データが初期化前`() = runTest {

        // before update (no data)
        coEvery { dao.getCurrentDataVersion() } returns null
        repository.getDataVersion()
        assertThat(repository.lastCheckedVersion).isNull()
        assertThat(repository.dataInitialized).isFalse()

        // check latest info
        coEvery { api.getLatestInfo() } returns info
        repository.getLatestDataVersion(forceRefresh = true)
        assertThat(repository.lastCheckedVersion).isEqualTo(info)
    }

    @Test
    fun `データのアップデート - 失敗`() = runTest {
        // watch flow
        val dataVersionList = mutableListOf<DataVersion?>()
        val job = launch(defaultDispatcher) {
            repository.dataVersion.toList(dataVersionList)
        }

        // update
        coEvery { useCase.invoke(any()) } returns DataUpdateResult.Failure
        val result = repository.updateData(info)
        assertThat(result).isEqualTo(DataUpdateResult.Failure)

        // verify data version flow
        assertThat(dataVersionList.last()).isNull()
        job.cancel()

        coVerify(exactly = 1) { useCase.invoke(info) }
    }


    @Test
    fun `データのアップデート - 成功`() = runTest {
        // watch flow
        val dataVersionList = mutableListOf<DataVersion?>()
        val job1 = launch(defaultDispatcher) {
            repository.dataVersion.toList(dataVersionList)
        }
        val progressList = mutableListOf<DataUpdateProgress>()
        val job2 = launch(defaultDispatcher) {
            repository.dataUpdateProgress.toList(progressList)
        }

        // update
        coEvery { useCase.invoke(any()) } coAnswers {
            updateProgress.emit(DataUpdateProgress.Download(0))
            updateProgress.emit(DataUpdateProgress.Download(100))
            updateProgress.emit(DataUpdateProgress.Save)
            DataUpdateResult.Success(
                DataVersion(version = info.version)
            )
        }
        val result = repository.updateData(info)
        assertThat(result).isInstanceOf(DataUpdateResult.Success::class.java)

        // verify progress flow
        assertThat(progressList).containsExactly(
            DataUpdateProgress.Download(0),
            DataUpdateProgress.Download(100),
            DataUpdateProgress.Save,
        ).inOrder()

        // verify data version flow
        assertThat(dataVersionList.size).isGreaterThan(1)
        assertThat(dataVersionList[0]).isNull()
        assertThat(dataVersionList.last()?.version).isEqualTo(info.version)
        job1.cancel()
        job2.cancel()

        coVerify(exactly = 1) { useCase.invoke(info) }
    }

    @Test
    fun `Daoの呼び出し`() = runTest {
        // prepare data
        val json = Json { ignoreUnknownKeys = true }
        val stream = javaClass.classLoader?.getResourceAsStream("data.json")
        val reader = BufferedReader(stream?.reader(Charsets.UTF_8))
        val str = reader.readText()
        val data = json.decodeFromString<StationData>(str)

        // mock dao operation
        val stationCodeSlot = slot<Int>()
        coEvery { dao.getStation(capture(stationCodeSlot)) } answers {
            val code = stationCodeSlot.captured
            data.stations.find { it.code == code } ?: throw NoSuchElementException()
        }
        val lineCodeSlot = slot<Int>()
        coEvery { dao.getLine(capture(lineCodeSlot)) } answers {
            val code = lineCodeSlot.captured
            data.lines.find { it.code == code } ?: throw NoSuchElementException()
        }
        coEvery { dao.getDataVersionHistory() } returns listOf(
            DataVersion(0),
            DataVersion(1),
        )

        // test
        val station = data.stations.random()
        assertThat(repository.getStation(station.code)).isEqualTo(station)
        val line = data.lines.random()
        assertThat(repository.getLine(line.code)).isEqualTo(line)
        val history = repository.getDataVersionHistory()
        assertThat(history.size).isEqualTo(2)

        // verify
        coVerifyOrder {
            dao.getStation(station.code)
            dao.getLine(line.code)
            dao.getDataVersionHistory()
        }
        confirmVerified(dao)
    }
}