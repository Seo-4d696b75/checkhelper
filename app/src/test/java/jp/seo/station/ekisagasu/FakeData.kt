package jp.seo.station.ekisagasu

import jp.seo.station.ekisagasu.model.DataLatestInfo
import jp.seo.station.ekisagasu.model.StationData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.BufferedReader

val <T : Any> T.fakeDataString: Lazy<String>
    get() = lazy {
        val stream = this.javaClass.classLoader?.getResourceAsStream("data.json")
        val reader = BufferedReader(stream?.reader(Charsets.UTF_8))
        reader.readText()
    }

val <T : Any> T.fakeLatestInfoString: Lazy<String>
    get() = lazy {
        val stream = this.javaClass.classLoader?.getResourceAsStream("latest_info.json")
        val reader = BufferedReader(stream?.reader(Charsets.UTF_8))
        reader.readText()
    }

val <T : Any> T.fakeData: Lazy<StationData>
    get() = lazy {
        val str by this.fakeDataString
        val json = Json { ignoreUnknownKeys = true }
        json.decodeFromString(str)
    }

val <T : Any> T.fakeLatestInfo: Lazy<DataLatestInfo>
    get() = lazy {
        val str by this.fakeLatestInfoString
        Json.decodeFromString(str)
    }
