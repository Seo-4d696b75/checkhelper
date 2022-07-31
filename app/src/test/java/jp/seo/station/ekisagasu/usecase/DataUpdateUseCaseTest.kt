@file:Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")

package jp.seo.station.ekisagasu.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.*
import jp.seo.station.ekisagasu.api.DownloadClient
import jp.seo.station.ekisagasu.database.DataVersion
import jp.seo.station.ekisagasu.database.StationDao
import jp.seo.station.ekisagasu.model.DataLatestInfo
import jp.seo.station.ekisagasu.model.DataUpdateProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.IOException
import kotlin.math.ceil

@ExperimentalSerializationApi
@ExperimentalCoroutinesApi
class DataUpdateUseCaseTest {
    private val defaultDispatcher = UnconfinedTestDispatcher()

    private val dao = mockk<StationDao>(relaxUnitFun = true)
    private val downloadClient = mockk<DownloadClient>()
    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var dataStr: String
    private lateinit var info: DataLatestInfo

    private lateinit var useCase: DataUpdateUseCase

    private val url = "https://test.com/data.json"
    private val version = 20220729L // src/test/resources/data.jsonに合わせる

    @Before
    fun setup() {
        val stream = javaClass.classLoader?.getResourceAsStream("data.json")
        val reader = BufferedReader(stream?.reader(Charsets.UTF_8))
        dataStr = reader.readText()
        info = DataLatestInfo(
            version = version,
            length = dataStr.toByteArray(Charsets.UTF_8).size.toLong(),
            url = url,
        )
        Dispatchers.setMain(defaultDispatcher)

        useCase = DataUpdateUseCase(dao, downloadClient, json)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `成功`() = runTest {
        // prepare
        val callbackSlot = slot<(Long)->Unit>()
        coEvery { downloadClient.invoke(any(), capture(callbackSlot)) } coAnswers {
            val callback = callbackSlot.captured
            callback(0L)
            callback(info.length)
            dataStr
        }
        coEvery { dao.getCurrentDataVersion() } returns DataVersion(version = info.version)

        // watch progress flow
        val progressList = mutableListOf<DataUpdateProgress>()
        val job = launch(defaultDispatcher) {
            useCase.progress.toList(progressList)
        }

        // test
        val result = useCase(info)

        // verify
        assertThat(result).isEqualTo(DataUpdateResult.Success(DataVersion(info.version)))
        coVerifyOrder {
            downloadClient.invoke(info.url, any())
            dao.updateData(any())
            dao.getCurrentDataVersion()
        }
        assertThat(progressList).containsExactly(
            DataUpdateProgress.Download(0),
            DataUpdateProgress.Download(100),
            DataUpdateProgress.Save
        ).inOrder()

        job.cancel()
        confirmVerified(downloadClient, dao)
    }

    @Test
    fun `ダウンロード失敗` () = runTest {
        // prepare
        val callbackSlot = slot<(Long)->Unit>()
        coEvery { downloadClient.invoke(any(), capture(callbackSlot)) } coAnswers {
            val callback = callbackSlot.captured
            callback(0L)
            callback(ceil(info.length / 2.0).toLong())
            throw IOException()
        }

        // watch progress flow
        val progressList = mutableListOf<DataUpdateProgress>()
        val job = launch(defaultDispatcher) {
            useCase.progress.toList(progressList)
        }

        // test
        val result = useCase(info)

        // verify
        assertThat(result).isEqualTo(DataUpdateResult.Failure)
        coVerifyOrder {
            downloadClient.invoke(info.url, any())
        }
        assertThat(progressList).containsExactly(
            DataUpdateProgress.Download(0),
            DataUpdateProgress.Download(50),
        ).inOrder()

        job.cancel()
        confirmVerified(downloadClient, dao)
    }
}