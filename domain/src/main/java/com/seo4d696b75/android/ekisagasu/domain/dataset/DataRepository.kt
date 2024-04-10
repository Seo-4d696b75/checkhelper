package com.seo4d696b75.android.ekisagasu.domain.dataset

import com.seo4d696b75.android.ekisagasu.domain.kdtree.StationKdTree
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * 駅・路線データへのアクセス＆データの更新方法を提供
 */
interface DataRepository {
    suspend fun getLine(code: Int): Line

    suspend fun getLines(codes: List<Int>): List<Line>

    suspend fun getStation(code: Int): Station

    suspend fun getStations(codes: List<Int>): List<Station>

    suspend fun getStationKdTree(): StationKdTree

    val dataInitialized: Boolean
    val dataVersion: StateFlow<DataVersion?>

    suspend fun getDataVersion(): DataVersion?

    suspend fun getDataVersionHistory(): List<DataVersion>

    suspend fun updateData(
        info: LatestDataVersion,
        dir: File,
    ): DataVersion
}
