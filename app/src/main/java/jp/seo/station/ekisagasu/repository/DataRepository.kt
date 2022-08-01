package jp.seo.station.ekisagasu.repository

import jp.seo.station.ekisagasu.database.DataVersion
import jp.seo.station.ekisagasu.model.*
import jp.seo.station.ekisagasu.usecase.DataUpdateResult
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 駅・路線データへのアクセス＆データの更新方法を提供
 */
interface DataRepository {
    suspend fun getLine(code: Int): Line
    suspend fun getLines(codes: List<Int>): List<Line>
    suspend fun getStation(code: Int): Station
    suspend fun getStations(codes: List<Int>): List<Station>
    suspend fun getTreeSegment(name: String): TreeSegment
    val dataInitialized: Boolean
    val dataVersion: StateFlow<DataVersion?>
    val lastCheckedVersion: DataLatestInfo?
    suspend fun getDataVersion(): DataVersion?
    suspend fun getLatestDataVersion(forceRefresh: Boolean = true): DataLatestInfo
    suspend fun getDataVersionHistory(): List<DataVersion>
    val dataUpdateProgress: SharedFlow<DataUpdateProgress>
    suspend fun updateData(info: DataLatestInfo): DataUpdateResult
}