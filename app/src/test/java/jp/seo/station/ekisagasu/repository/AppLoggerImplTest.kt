package jp.seo.station.ekisagasu.repository

import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.Status
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import jp.seo.station.ekisagasu.model.AppMessage
import jp.seo.station.ekisagasu.repository.impl.AppLoggerImpl
import jp.seo.station.ekisagasu.repository.impl.AppStateRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AppLoggerImplTest {
    private val logRepository = mockk<LogRepository>()

    private lateinit var appStateRepository: AppStateRepository
    private lateinit var logger: AppLogger

    @Before
    fun setup() {
        appStateRepository = AppStateRepositoryImpl(logRepository)
        logger = AppLoggerImpl(appStateRepository)
        coEvery { logRepository.saveMessage(any()) } returns Unit
    }

    @Test
    fun emitMessage() = runTest {
        // prepare
        val messageList = mutableListOf<AppMessage>()
        val job = launch {
            appStateRepository.message.toList(messageList)
        }

        // test
        logger.apply {
            log("log")
            error("error")
            requestExceptionResolved("error", ResolvableApiException(Status.RESULT_CANCELED))
        }

        advanceUntilIdle()

        // verify
        coVerifyOrder {
            logRepository.saveMessage(ofType(AppMessage.Log::class))
            logRepository.saveMessage(ofType(AppMessage.Error::class))
            logRepository.saveMessage(ofType(AppMessage.ResolvableException::class))
        }
        assertThat(messageList.size).isEqualTo(3)
        assertThat(messageList[0]).isInstanceOf(AppMessage.Log::class.java)
        assertThat(messageList[1]).isInstanceOf(AppMessage.Error::class.java)
        assertThat(messageList[2]).isInstanceOf(AppMessage.ResolvableException::class.java)
        job.cancel()
    }
}
