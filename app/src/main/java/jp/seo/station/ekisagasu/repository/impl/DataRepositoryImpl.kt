package jp.seo.station.ekisagasu.repository.impl

import jp.seo.station.ekisagasu.database.DataVersion
import jp.seo.station.ekisagasu.database.StationDao
import jp.seo.station.ekisagasu.model.DataLatestInfo
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.model.Station
import jp.seo.station.ekisagasu.model.StationKdTree
import jp.seo.station.ekisagasu.repository.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

class DataRepositoryImpl
    @Inject
    constructor(
        private val dao: StationDao,
        private val json: Json,
    ) : DataRepository {
        override suspend fun getLine(code: Int) =
            withContext(Dispatchers.IO) {
                dao.getLine(code)
            }

        override suspend fun getLines(codes: List<Int>) =
            withContext(Dispatchers.IO) {
                dao.getLines(codes)
            }

        override suspend fun getStation(code: Int) =
            withContext(Dispatchers.IO) {
                dao.getStation(code)
            }

        override suspend fun getStations(codes: List<Int>) =
            withContext(Dispatchers.IO) {
                dao.getStations(codes)
            }

        override suspend fun getStationKdTree() =
            withContext(Dispatchers.IO) {
                StationKdTree(
                    root = dao.getRootStationNode().code,
                    nodes = dao.getStationNodes(),
                )
            }

        private val _currentVersion = MutableStateFlow<DataVersion?>(null)
        private var _dataInitialized: Boolean = false

        override val dataInitialized: Boolean
            get() = _dataInitialized

        override val dataVersion: StateFlow<DataVersion?> = _currentVersion

        override suspend fun getDataVersion(): DataVersion? =
            withContext(Dispatchers.IO) {
                val version = dao.getCurrentDataVersion()
                _dataInitialized = version != null
                _currentVersion.value = version
                version
            }

        override suspend fun getDataVersionHistory() = dao.getDataVersionHistory()

        override suspend fun updateData(
            info: DataLatestInfo,
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
            )

        private fun File.lines(): List<Line> {
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
            }
        }

        private fun File.kdTree() =
            json.decodeFromString<StationKdTree>(
                File(this, "json/tree.json").readText(Charsets.UTF_8),
            )
    }
