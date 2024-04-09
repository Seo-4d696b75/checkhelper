package com.seo4d696b75.android.ekisagasu.data.api

import com.seo4d696b75.android.ekisagasu.data.station.LatestDataVersion
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

/**
 * @author Seo-4d696b75
 * @version 2020/12/18.
 */
interface APIClient {
    @GET("station_database/latest_info.json")
    suspend fun getLatestInfo(): LatestDataVersion

    @Streaming
    @GET("station_database@{version}/out/main/json.zip")
    suspend fun getLatestData(
        @Path("version") version: Long,
    ): ResponseBody
}
