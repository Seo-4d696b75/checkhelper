package jp.seo.station.ekisagasu.api

import com.google.common.truth.Truth.assertThat
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import jp.seo.station.ekisagasu.fakeDataString
import jp.seo.station.ekisagasu.fakeLatestInfoString
import jp.seo.station.ekisagasu.model.PolylineSegment
import jp.seo.station.ekisagasu.model.StationData
import jp.seo.station.ekisagasu.model.convertGeoJsonFeature
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import kotlin.random.Random


/**
 * @author Seo-4d696b75
 * @version 2020/12/26.
 */
@ExperimentalSerializationApi
@ExperimentalCoroutinesApi
class APITest {

    private val server = MockWebServer()
    private val info by fakeLatestInfoString
    private val data by fakeDataString

    private val serverDispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            return when (request.path) {
                "/latest_info.json" -> MockResponse().setBody(info).setResponseCode(200)
                "/out/main/data.json" -> MockResponse().setBody(data).setResponseCode(200)
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

        val client = DownloadClientImpl(api)
        val callback = mockk<(Long) -> Unit>()
        every { callback.invoke(any()) } returns Unit

        val data = client(info.url, callback).let { json.decodeFromString<StationData>(it) }
        assertThat(data.version).isEqualTo(info.version)
        verifyOrder {
            callback(0L)
            callback(info.length)
        }

        assertData(data)
    }

    private fun assertData(data: StationData) {

        val stations = data.stations
        val lines = data.lines

        // validation stations and lines
        val idPattern = Regex("[0-9a-f]{6}").toPattern()
        val colorPattern = Regex("#[0-9a-fA-F]{6}").toPattern()
        val attrs = listOf("cool", "heat", "eco")
        for (s in stations) {
            assertThat(s.id).matches(idPattern)
            assertThat(s.code).isGreaterThan(0)
            assertThat(s.code).isLessThan(10000000)
            assertThat(s.lat).isGreaterThan(23.0)
            assertThat(s.lat).isLessThan(46.0)
            assertThat(s.lng).isGreaterThan(125.0)
            assertThat(s.lng).isLessThan(146.0)
            assertThat(s.name).contains(s.originalName)
            assertThat(s.originalName).isNotEmpty()
            assertThat(s.nameKana).isNotEmpty()
            assertThat(s.prefecture).isGreaterThan(0)
            assertThat(s.prefecture).isLessThan(48)
            assertThat(s.lines).isNotEmpty()
            assertThat(s.next).isNotEmpty()
            if (s.closed) {
                assertThat(s.attr).isEqualTo("unknown")
            } else {
                assertThat(s.attr).isIn(attrs)
            }
        }

        for (line in lines) {
            assertThat(line.id).matches(idPattern)
            assertThat(line.code).isGreaterThan(0)
            assertThat(line.code).isLessThan(100000)
            assertThat(line.name).isNotEmpty()
            assertThat(line.nameKana).isNotEmpty()
            assertThat(line.stationSize).isGreaterThan(0)
            assertThat(line.stationList.size).isEqualTo(line.stationSize)
            line.color?.let { assertThat(it).matches(colorPattern) }
            line.symbol?.let { assertThat(it).isNotEmpty() }
            if (!line.closed) assertThat(line.polyline).isNotNull()
        }

        // geojson format cannot be validated as type: JsonObject
        val types = arrayOf("Polygon", "LineString").toList()
        val rand = Random(System.currentTimeMillis())
        for (i in 1..(stations.size / 100)) {
            val s = stations[rand.nextInt(stations.size)]
            val f = convertGeoJsonFeature(s.voronoi)
            assertThat(f.geometry.geometryType).isIn(types)
        }
        for (i in 1..(lines.size / 100)) {
            val line = lines[rand.nextInt(lines.size)]
            line.polyline?.let {
                val seg = PolylineSegment.parseSegments(it)
                assertThat(seg.size).isGreaterThan(0)
            }
        }

    }
}
