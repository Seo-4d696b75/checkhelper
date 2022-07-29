package jp.seo.station.ekisagasu.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import jp.seo.station.ekisagasu.model.DataLatestInfo
import jp.seo.station.ekisagasu.model.StationData
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Okio
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url


/**
 * @author Seo-4d696b75
 * @version 2020/12/18.
 */
interface APIClient {
    @GET("latest_info.json")
    suspend fun getLatestInfo(): DataLatestInfo

    @GET("out/main/data.json")
    suspend fun getLatestData(): StationData

    @GET
    suspend fun getData(@Url url: String): StationData
}

@ExperimentalSerializationApi
fun getAPIClient(baseURL: String, json: Json): APIClient {
    val client = OkHttpClient.Builder().build()
    val contentType = MediaType.get("application/json")
    val converter = json.asConverterFactory(contentType)
    val retrofit = Retrofit.Builder()
        .baseUrl(baseURL)
        .client(client)
        .addConverterFactory(converter)
        .build()
    return retrofit.create(APIClient::class.java)
}

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

@ExperimentalSerializationApi
fun getDownloadClient(json: Json, listener: (Long) -> Unit): APIClient {

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

    val contentType = MediaType.get("application/json")
    val converter = json.asConverterFactory(contentType)
    val retrofit = Retrofit.Builder()
        .baseUrl("http://hoge.com")
        .client(client)
        .addConverterFactory(converter)
        .build()
    return retrofit.create(APIClient::class.java)
}


