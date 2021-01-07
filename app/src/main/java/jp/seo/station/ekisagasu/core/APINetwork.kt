package jp.seo.station.ekisagasu.core

import com.google.android.gms.maps.model.LatLng
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.LineConverter
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.StationConverter
import jp.seo.station.ekisagasu.search.TreeSegment
import jp.seo.station.ekisagasu.utils.PositionJsonConverter
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

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


