package com.seo4d696b75.android.ekisagasu.data

import com.seo4d696b75.android.ekisagasu.data.station.LineResponse
import com.seo4d696b75.android.ekisagasu.data.station.StationResponse
import com.seo4d696b75.android.ekisagasu.domain.dataset.LatestDataVersion
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Prefecture
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.kdtree.StationKdTree
import kotlinx.serialization.Serializable
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

internal val <T : Any> T.fakeStations: Lazy<List<StationResponse>>
    get() =
        lazy {
            val stream = this.javaClass.classLoader?.getResourceAsStream("json/station.json")
            val str = BufferedReader(stream?.reader(Charsets.UTF_8)).readText()
            json.decodeFromString<List<StationResponse>>(str)
        }

internal fun StationResponse.toModel() =
    Station(
        id = id,
        code = code,
        lat = lat,
        lng = lng,
        name = name,
        originalName = originalName,
        nameKana = nameKana,
        prefecture = Prefecture(prefecture, "name"),
        lines = lines.map { code ->
            val lines by fakeLines
            lines.find { it.code == code }?.toModel() ?: throw NoSuchElementException()
        },
        closed = closed,
        voronoi = voronoi,
    )

internal val <T : Any> T.fakeLines: Lazy<List<LineResponse>>
    get() =
        lazy {
            fakeLineCodes.value.map {
                val stream = this.javaClass.classLoader?.getResourceAsStream("json/line/$it.json")
                val str = BufferedReader(stream?.reader(Charsets.UTF_8)).readText()
                json.decodeFromString<LineResponse>(str)
            }
        }

internal fun LineResponse.toModel() =
    Line(
        id = id,
        code = code,
        name = name,
        nameKana = nameKana,
        stationSize = stationSize,
        symbol = symbol,
        color = color,
        closed = closed,
        stationList = stationList,
        polyline = polyline,
    )

@Serializable
private data class LineCode(val code: Int)

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
