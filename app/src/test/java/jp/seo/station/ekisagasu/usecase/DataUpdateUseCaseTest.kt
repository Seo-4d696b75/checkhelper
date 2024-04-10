@file:Suppress("NonAsciiCharacters", "RemoveRedundantBackticks")

package jp.seo.station.ekisagasu.usecase

import com.google.common.truth.Truth.assertThat
import com.seo4d696b75.android.ekisagasu.data.database.DataVersion
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateProgress
import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateUseCase
import com.seo4d696b75.android.ekisagasu.domain.dataset.RemoteDataRepository
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot
import jp.seo.station.ekisagasu.fakeLatestInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import kotlin.math.ceil

@ExperimentalCoroutinesApi
class DataUpdateUseCaseTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val defaultDispatcher = UnconfinedTestDispatcher()

    private val repository = mockk<DataRepository>()
    private val remoteRepository = mockk<RemoteDataRepository>()

    private val info by fakeLatestInfo

    private lateinit var useCase: DataUpdateUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(defaultDispatcher)
        useCase = DataUpdateUseCase(repository, remoteRepository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `成功`() =
        runTest(defaultDispatcher) {
            // prepare
            val callbackSlot = slot<(Long) -> Unit>()
            coEvery { remoteRepository.download(any(), any(), capture(callbackSlot)) } coAnswers {
                val callback = callbackSlot.captured
                callback(0L)
                callback(info.length)
            }
            coEvery { repository.updateData(any(), any()) } returns DataVersion(version = info.version)

            // watch progress flow
            val progressList = mutableListOf<DataUpdateProgress>()
            val job =
                launch {
                    useCase.progress.toList(progressList)
                }

            // test
            val result = useCase(info, tempFolder.newFolder())

            // verify
            assertThat(result.getOrNull()).isEqualTo(DataVersion(info.version))
            coVerifyOrder {
                remoteRepository.download(info.version, any(), any())
                repository.updateData(info, any())
            }
            assertThat(progressList).containsExactly(
                DataUpdateProgress.Download(0),
                DataUpdateProgress.Download(100),
                DataUpdateProgress.Save,
            ).inOrder()

            job.cancel()
            confirmVerified(remoteRepository, repository)
        }

    @Test
    fun `ダウンロード失敗`() =
        runTest {
            // prepare
            val callbackSlot = slot<(Long) -> Unit>()
            coEvery { remoteRepository.download(any(), any(), capture(callbackSlot)) } coAnswers {
                val callback = callbackSlot.captured
                callback(0L)
                callback(ceil(info.length / 2.0).toLong())
                throw IOException()
            }

            // watch progress flow
            val progressList = mutableListOf<DataUpdateProgress>()
            val job =
                launch(defaultDispatcher) {
                    useCase.progress.toList(progressList)
                }

            // test
            val result = useCase(info, tempFolder.newFolder())

            // verify
            assertThat(result.isSuccess).isFalse()
            coVerifyOrder {
                remoteRepository.download(info.version, any(), any())
            }
            assertThat(progressList).containsExactly(
                DataUpdateProgress.Download(0),
                DataUpdateProgress.Download(50),
            ).inOrder()

            job.cancel()
            confirmVerified(repository, remoteRepository)
        }
}
