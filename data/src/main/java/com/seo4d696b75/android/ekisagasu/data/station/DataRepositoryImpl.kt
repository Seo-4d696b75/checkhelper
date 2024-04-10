package com.seo4d696b75.android.ekisagasu.data.station

import com.seo4d696b75.android.ekisagasu.data.database.station.LineEntity
import com.seo4d696b75.android.ekisagasu.data.database.station.StationDao
import com.seo4d696b75.android.ekisagasu.data.database.station.StationEntity
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataVersion
import com.seo4d696b75.android.ekisagasu.domain.dataset.LatestDataVersion
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.kdtree.StationKdTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

class DataRepositoryImpl @Inject constructor(
    private val dao: StationDao,
    private val json: Json,
) : DataRepository {
    override suspend fun getLine(code: Int) =
        withContext(Dispatchers.IO) {
            dao.getLine(code).toModel()
        }

    override suspend fun getLines(codes: List<Int>) =
        withContext(Dispatchers.IO) {
            dao.getLines(codes).map { it.toModel() }
        }

    override suspend fun getStation(code: Int) =
        withContext(Dispatchers.IO) {
            dao.getStation(code).toModel()
        }

    override suspend fun getStations(codes: List<Int>) =
        withContext(Dispatchers.IO) {
            dao.getStations(codes).map { it.toModel() }
        }

    override suspend fun getStationKdTree() =
        withContext(Dispatchers.IO) {
            StationKdTree(
                root = dao.getRootStationNode().code,
                nodes = dao.getStationNodes().map { it.toModel() },
            )
        }

    // TODO not cache in repository!
    private val _currentVersion = MutableStateFlow<DataVersion?>(null)
    private var _dataInitialized: Boolean = false

    override val dataInitialized: Boolean
        get() = _dataInitialized

    override val dataVersion: StateFlow<DataVersion?> = _currentVersion

    override suspend fun getDataVersion(): DataVersion? =
        withContext(Dispatchers.IO) {
            val version = dao.getCurrentDataVersion()?.toModel()
            _dataInitialized = version != null
            _currentVersion.value = version
            version
        }

    override suspend fun getDataVersionHistory() = dao.getDataVersionHistory().map { it.toModel() }

    override suspend fun updateData(
        info: LatestDataVersion,
        dir: File,
    ) = withContext(Dispatchers.IO) {
        val stations = dir.stations()
        val lines = dir.lines()
        val tree = dir.kdTree()
        val version = dao.updateData(info.version, stations, lines, tree)
        _dataInitialized = true
        _currentVersion.value = version
        version
    }

    private fun File.stations() =
        json.decodeFromString<List<Station>>(
            File(this, "json/station.json").readText(Charsets.UTF_8),
        ).map { StationEntity.fromModel(it) }

    private fun File.lines(): List<LineEntity> {
        val dir = File(this, "json/line")
        require(dir.exists() && dir.isDirectory)
        return requireNotNull(dir.listFiles()).map {
            val line = json.decodeFromString<Line>(it.readText(Charsets.UTF_8))
            // load polyline from different file
            val file = File(this, "json/polyline/${line.code}.json")
            if (file.exists()) {
                line.copy(polyline = file.readText(Charsets.UTF_8))
            } else {
                line
            }
        }.map { LineEntity.fromModel(it) }
    }

    private fun File.kdTree() =
        json.decodeFromString<StationKdTree>(
            File(this, "json/tree.json").readText(Charsets.UTF_8),
        )
}
