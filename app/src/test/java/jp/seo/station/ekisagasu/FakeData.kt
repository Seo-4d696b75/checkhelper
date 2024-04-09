package jp.seo.station.ekisagasu

import com.seo4d696b75.android.ekisagasu.data.kdtree.StationKdTree
import com.seo4d696b75.android.ekisagasu.data.station.LatestDataVersion
import com.seo4d696b75.android.ekisagasu.data.station.Line
import com.seo4d696b75.android.ekisagasu.data.station.Station
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okio.Buffer
import java.io.BufferedReader

fun <T : Any> T.fakeData() =
    requireNotNull(
        this.javaClass.classLoader?.getResourceAsStream("json.zip"),
    )

fun <T : Any> T.fakeDataBuffer() =
    requireNotNull(
        this.javaClass.classLoader?.getResourceAsStream("json.zip"),
    ).let {
        Buffer().apply { write(it.readBytes()) }
    }

val <T : Any> T.fakeLatestInfoString: Lazy<String>
    get() =
        lazy {
            val stream = this.javaClass.classLoader?.getResourceAsStream("latest_info.json")
            val reader = BufferedReader(stream?.reader(Charsets.UTF_8))
            reader.readText()
        }

private val json = Json { ignoreUnknownKeys = true }

val <T : Any> T.fakeStations: Lazy<List<Station>>
    get() =
        lazy {
            val stream = this.javaClass.classLoader?.getResourceAsStream("json/station.json")
            val str = BufferedReader(stream?.reader(Charsets.UTF_8)).readText()
            json.decodeFromString<List<Station>>(str)
        }

val <T : Any> T.fakeLines: Lazy<List<Line>>
    get() =
        lazy {
            fakeLineCodes.value.map {
                val stream = this.javaClass.classLoader?.getResourceAsStream("json/line/$it.json")
                val str = BufferedReader(stream?.reader(Charsets.UTF_8)).readText()
                json.decodeFromString<Line>(str)
            }
        }

@Serializable
private data class LineCode(
    val code: Int,
)

val <T : Any> T.fakeLineCodes: Lazy<List<Int>>
    get() =
        lazy {
            val stream = this.javaClass.classLoader?.getResourceAsStream("json/line.json")
            val str = BufferedReader(stream?.reader(Charsets.UTF_8)).readText()
            json.decodeFromString<List<LineCode>>(str).map { it.code }
        }

val <T : Any> T.fakeTree: Lazy<StationKdTree>
    get() =
        lazy {
            val stream = this.javaClass.classLoader?.getResourceAsStream("json/tree.json")
            val str = BufferedReader(stream?.reader(Charsets.UTF_8)).readText()
            json.decodeFromString<StationKdTree>(str)
        }

val <T : Any> T.fakeLatestInfo: Lazy<LatestDataVersion>
    get() =
        lazy {
            val str by this.fakeLatestInfoString
            json.decodeFromString(str)
        }
