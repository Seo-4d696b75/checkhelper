package jp.seo.station.ekisagasu.api

import com.google.common.truth.Truth.assertThat
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.mockk
import io.mockk.verifyOrder
import jp.seo.station.ekisagasu.fakeDataBuffer
import jp.seo.station.ekisagasu.fakeLatestInfoString
import jp.seo.station.ekisagasu.repository.impl.RemoteDataRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit

/**
 * @author Seo-4d696b75
 * @version 2020/12/26.
 */
// @Ignore("GithubActionsだとメモリ不足で落ちる")
@ExperimentalCoroutinesApi
class APITest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val server = MockWebServer()
    private val info by fakeLatestInfoString

    private val serverDispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            return when (request.path) {
                "/station_database/latest_info.json" -> MockResponse().setBody(info)
                "/station_database@20240329/out/main/json.zip" -> MockResponse().setBody(fakeDataBuffer())
                else -> MockResponse().setResponseCode(404)
            }
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private lateinit var retrofit: Retrofit

    @Before
    fun setup() {
        server.dispatcher = serverDispatcher
        server.start()
        val url = server.url("")
        val client = OkHttpClient.Builder().build()
        val contentType = "application/json".toMediaType()
        val converter = json.asConverterFactory(contentType)
        retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(converter)
            .build()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun testAPIClient() = runTest {
        val api = retrofit.create(APIClient::class.java)

        val info = api.getLatestInfo()
        assertThat(info.version.toString()).matches(Regex("^[0-9]{8}$").toPattern())

        val repository = RemoteDataRepositoryImpl(api)
        val dir = tempFolder.newFolder()

        val callback = mockk<(Long) -> Unit>(relaxed = true)
        repository.download(info.version, dir, callback)
        verifyOrder {
            callback(0L)
            callback(info.length)
        }
    }
}
