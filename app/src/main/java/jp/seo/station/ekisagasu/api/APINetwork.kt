package jp.seo.station.ekisagasu.api

import jp.seo.station.ekisagasu.model.DataLatestInfo
import jp.seo.station.ekisagasu.model.StationData
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * @author Seo-4d696b75
 * @version 2020/12/18.
 */
interface APIClient {
    @GET("latest_info.json")
    suspend fun getLatestInfo(): DataLatestInfo

    @GET("out/main/data.json")
    suspend fun getLatestData(): StationData

    @Streaming
    @GET
    suspend fun getData(@Url url: String): ResponseBody
}

interface DownloadClient {
    /**
     * 指定したURLからダウンロードする
     *
     * @param url URL文字列
     * @param callback ダウンロード中に現在までに取得したbyte長を通知する
     */
    suspend operator fun invoke(url: String, callback: (Long) -> Unit): String
}

@ExperimentalSerializationApi
class DownloadClientImpl @Inject constructor(
    private val api: APIClient,
) : DownloadClient {
    // https://stackoverflow.com/questions/42118924/android-retrofit-download-progress
    override suspend operator fun invoke(url: String, callback: (Long) -> Unit): String {
        callback(0L)
        api.getData(url).also { res ->
            res.byteStream().use { inputStream ->
                val outputStream = ByteArrayOutputStream()
                var bytes = 0L
                val buf = ByteArray(8192)
                while (true) {
                    val read = inputStream.read(buf)
                    if (read < 0) {
                        break
                    }
                    bytes += read
                    outputStream.write(buf, 0, read)
                    callback(bytes)
                }
                return String(outputStream.toByteArray())
            }
        }
    }
}
