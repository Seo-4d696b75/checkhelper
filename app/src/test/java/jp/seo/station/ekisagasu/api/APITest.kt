package jp.seo.station.ekisagasu.api

import com.google.common.truth.Truth.assertThat
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import jp.seo.station.ekisagasu.fakeDataString
import jp.seo.station.ekisagasu.model.PolylineSegment
import jp.seo.station.ekisagasu.model.StationData
import jp.seo.station.ekisagasu.model.convertGeoJsonFeature
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.io.BufferedReader
import kotlin.random.Random


/**
 * @author Seo-4d696b75
 * @version 2020/12/26.
 */
@ExperimentalSerializationApi
@ExperimentalCoroutinesApi
class APITest {

    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val baseURL = "https://raw.githubusercontent.com/Seo-4d696b75/station_database/main/"
    private lateinit var retrofit: Retrofit

    @Before
    fun setup() {
        val client = OkHttpClient.Builder().build()
        val contentType = MediaType.get("application/json")
        val converter = json.asConverterFactory(contentType)
        retrofit = Retrofit.Builder()
            .baseUrl(baseURL)
            .client(client)
            .addConverterFactory(converter)
            .build()
    }

    @Test
    fun testJsonDecode() {
        // ローカルのデータでデコード処理
        val str by fakeDataString
        val data = json.decodeFromString<StationData>(str)
        assertThat(data.version).isEqualTo(20220729)

        assertData(data)
    }

    @Test
    fun testAPIClient() = runTest {
        // 実際にネットワーク上のAPIを呼び出す
        val api = retrofit.create(APIClient::class.java)
        val data = api.getLatestData()

        assertData(data)
    }

    @Test
    fun testDownloadClient() = runTest {
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
