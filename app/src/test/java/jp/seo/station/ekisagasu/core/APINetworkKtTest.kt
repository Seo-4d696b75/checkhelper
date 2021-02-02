package jp.seo.station.ekisagasu.core

import com.google.common.truth.Truth.assertThat
import jp.seo.station.ekisagasu.utils.PolylineSegment
import jp.seo.station.ekisagasu.utils.convertGeoJsonFeature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.random.Random


/**
 * @author Seo-4d696b75
 * @version 2020/12/26.
 */
class APINetworkKtTest {

    @Test
    fun testAPIClient() = runBlocking(Dispatchers.IO) {
        // select a url of appropriate branch
        val client =
            getAPIClient("https://raw.githubusercontent.com/Seo-4d696b75/station_database/master/")
        val info = client.getLatestInfo()
        assertThat(info.version.toString()).matches(Regex("^[0-9]{8}$").toPattern())
        val latest = client.getLatestData()
        assertThat(latest.version).isEqualTo(info.version)
        val data = client.getData(info.url)
        assertThat(data.version).isEqualTo(info.version)

        val stations = data.stations
        val lines = data.lines

        // validation stations and lines
        val idPattern = Regex("[0-9a-f]{6}").toPattern()
        val colorPattern = Regex("#[0-9a-fA-F]{6}").toPattern()
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
