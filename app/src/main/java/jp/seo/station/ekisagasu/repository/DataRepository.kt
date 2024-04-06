package jp.seo.station.ekisagasu.repository

import jp.seo.station.ekisagasu.database.DataVersion
import jp.seo.station.ekisagasu.model.DataLatestInfo
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.model.Station
import jp.seo.station.ekisagasu.model.StationKdTree
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
    suspend fun updateData(info: DataLatestInfo, dir: File): DataVersion
}
