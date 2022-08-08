@file:Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")

package jp.seo.station.ekisagasu.usecase

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot
import jp.seo.station.ekisagasu.api.DownloadClient
import jp.seo.station.ekisagasu.database.DataVersion
import jp.seo.station.ekisagasu.database.StationDao
import jp.seo.station.ekisagasu.fakeDataString
import jp.seo.station.ekisagasu.fakeLatestInfo
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
import java.io.IOException
import kotlin.math.ceil

@ExperimentalSerializationApi
@ExperimentalCoroutinesApi
class DataUpdateUseCaseTest {
    private val defaultDispatcher = UnconfinedTestDispatcher()

    private val dao = mockk<StationDao>(relaxUnitFun = true)
    private val downloadClient = mockk<DownloadClient>()
    private val json = Json { ignoreUnknownKeys = true }

    private val dataStr by fakeDataString
    private val info by fakeLatestInfo

    private lateinit var useCase: DataUpdateUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(defaultDispatcher)

        useCase = DataUpdateUseCase(dao, downloadClient, json)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `成功`() = runTest(defaultDispatcher) {
        // prepare
        val callbackSlot = slot<(Long) -> Unit>()
        coEvery { downloadClient.invoke(any(), capture(callbackSlot)) } coAnswers {
            val callback = callbackSlot.captured
            callback(0L)
            callback(info.length)
            dataStr
        }
        coEvery { dao.getCurrentDataVersion() } returns DataVersion(version = info.version)

        // watch progress flow
        val progressList = mutableListOf<DataUpdateProgress>()
        val job = launch {
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
    fun `ダウンロード失敗`() = runTest {
        // prepare
        val callbackSlot = slot<(Long) -> Unit>()
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
