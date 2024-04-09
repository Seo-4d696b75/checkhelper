package com.seo4d696b75.android.ekisagasu.data.kdtree

import com.seo4d696b75.android.ekisagasu.data.station.Station

/**
 * 近傍探索
 */
interface NearestSearch {
    /**
     * @param lat 緯度
     * @param lng 軽度
     * @param k 近傍のk個の点を探索する
     * @param r 指定点から半径r以内のすべての近傍点を探索する
     * @param sphere trueなら測地線距離で遠近を判定, falseならユークリッド距離
     */
    suspend fun search(
        lat: Double,
        lng: Double,
        k: Int,
        r: Double,
        sphere: Boolean = false,
    ): SearchResult
}

data class SearchResult(
    val lat: Double,
    val lng: Double,
    val k: Int,
    val r: Double,
    val stations: List<Station>,
)
