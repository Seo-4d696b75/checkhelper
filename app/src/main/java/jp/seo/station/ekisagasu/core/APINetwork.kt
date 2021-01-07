package jp.seo.station.ekisagasu.core

import android.os.Handler
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.LineConverter
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.StationConverter
import jp.seo.station.ekisagasu.search.TreeSegment
import jp.seo.station.ekisagasu.core.StationRepository.UpdateProgressListener
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okio.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import kotlin.math.floor


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
    val fileSize: String,
    @SerializedName("url")
    @Expose
    val url: String
)

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

class ProgressResponseBody(
    private val adapt: ResponseBody,
    private val listener: UpdateProgressListener,
    private val main: Handler
) : ResponseBody() {

    private var buf: BufferedSource? = null

    override fun contentType(): MediaType? = adapt.contentType()

    override fun contentLength(): Long = adapt.contentLength()

    override fun source(): BufferedSource {
        return buf ?: kotlin.run {
            val s = object : ForwardingSource(adapt.source()) {
                private var totalBytes: Long = 0L
                private var lastPercent: Int = -1
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val readBytes = super.read(sink, byteCount)
                    if (readBytes == -1L) {
                        main.post{
                            listener.onProgress(100)
                            listener.onStateChanged(UpdateProgressListener.STATE_PARSE)
                        }
                    } else {
                        totalBytes += readBytes
                        val percent = floor(totalBytes.toFloat() / contentLength() * 100.0f).toInt()

                        if (percent > lastPercent) {
                            lastPercent = percent
                            main.post { listener.onProgress(percent) }
                        }
                    }
                    return readBytes
                }
            }
            val b = Okio.buffer(s)
            buf = b
            b
        }
    }

}

fun getDownloadClient(listener: UpdateProgressListener, main: Handler): APIClient {

    val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val res = chain.proceed(chain.request())
            val body = res.body()
            body?.let {
                res.newBuilder()
                    .body(ProgressResponseBody(it, listener, main))
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


