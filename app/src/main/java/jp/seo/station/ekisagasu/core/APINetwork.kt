package jp.seo.station.ekisagasu.core

import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.LineConverter
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.StationConverter
import jp.seo.station.ekisagasu.search.TreeSegment
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Okio
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import java.text.StringCharacterIterator


/**
 * @author Seo-4d696b75
 * @version 2020/12/18.
 */
interface APIClient {
    @GET("latest_info.json")
    suspend fun getLatestInfo(): DataLatestInfo

    @GET("out/data.json")
    suspend fun getLatestData(): StationData

    @GET
    suspend fun getData(@Url url: String): StationData
}

fun getAPIClient(baseURL: String): APIClient {
    val client = OkHttpClient.Builder().build()
    val gson = GsonBuilder()
        .registerTypeAdapter(Station::class.java, StationConverter())
        .registerTypeAdapter(Line::class.java, LineConverter())
        .serializeNulls()
        .create()
    val converter = GsonConverterFactory.create(gson)
    val retrofit = Retrofit.Builder()
        .baseUrl(baseURL)
        .client(client)
        .addConverterFactory(converter)
        .build()
    return retrofit.create(APIClient::class.java)
}

data class DataLatestInfo(
    @SerializedName("version")
    @Expose
    val version: Long,
    @SerializedName("size")
    @Expose
    val length: Long,
    @SerializedName("url")
    @Expose
    val url: String
) {
    fun fileSize(): String {
        var bytes = length
        if (bytes < 0) return "0 B"
        if (bytes < 1000) return "$bytes B"
        val ci = StringCharacterIterator("KMGTPE")
        while (bytes >= 999_950) {
            bytes /= 1000
            ci.next()
        }
        return String.format("%.1f %cB", bytes.toFloat() / 1000.0f, ci.current())
    }
}

data class StationData(
    @SerializedName("version")
    @Expose
    val version: Long,
    @SerializedName("stations")
    @Expose
    val stations: List<Station>,
    @SerializedName("lines")
    @Expose
    val lines: List<Line>,
    @SerializedName("tree_segments")
    @Expose
    val trees: List<TreeSegment>
)

// https://stackoverflow.com/questions/42118924/android-retrofit-download-progress
class ProgressResponseBody(
    private val adapt: ResponseBody,
    private val listener: (Long) -> Unit,
) : ResponseBody() {

    private var buf: BufferedSource? = null

    override fun contentType(): MediaType? = adapt.contentType()

    override fun contentLength(): Long = adapt.contentLength()

    override fun source(): BufferedSource {
        return buf ?: kotlin.run {
            val s = object : ForwardingSource(adapt.source()) {
                private var totalBytes: Long = 0L
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val readBytes = super.read(sink, byteCount)
                    if (readBytes > 0L) totalBytes += readBytes
                    listener(totalBytes)
                    return readBytes
                }
            }
            val b = Okio.buffer(s)
            buf = b
            b
        }
    }

}

fun getDownloadClient(listener: (Long) -> Unit): APIClient {

    val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val res = chain.proceed(chain.request())
            val body = res.body()
            body?.let {
                res.newBuilder()
                    .body(ProgressResponseBody(it, listener))
                    .build()
            } ?: res
        }.build()
    val gson = GsonBuilder()
        .registerTypeAdapter(Station::class.java, StationConverter())
        .registerTypeAdapter(Line::class.java, LineConverter())
        .serializeNulls()
        .create()
    val converter = GsonConverterFactory.create(gson)
    val retrofit = Retrofit.Builder()
        .baseUrl("http://hoge.com")
        .client(client)
        .addConverterFactory(converter)
        .build()
    return retrofit.create(APIClient::class.java)
}


