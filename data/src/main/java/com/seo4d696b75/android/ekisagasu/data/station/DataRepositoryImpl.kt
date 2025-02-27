package com.seo4d696b75.android.ekisagasu.data.station

import com.seo4d696b75.android.ekisagasu.data.database.station.LineEntity
import com.seo4d696b75.android.ekisagasu.data.database.station.StationDao
import com.seo4d696b75.android.ekisagasu.data.database.station.StationEntity
import com.seo4d696b75.android.ekisagasu.domain.dataset.ColorInt
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataVersionState
import com.seo4d696b75.android.ekisagasu.domain.dataset.LatestDataVersion
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.PrefectureRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.kdtree.StationKdTree
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

class DataRepositoryImpl @Inject constructor(
    private val dao: StationDao,
    private val json: Json,
    private val prefectureRepository: PrefectureRepository,
) : DataRepository {
    override suspend fun getLine(code: Int) =
        withContext(Dispatchers.IO) {
            dao.getLine(code).toModel()
        }

    private fun LineEntity.toModel() = Line(
        id = id,
        code = code,
        name = name,
        nameKana = nameKana,
        stationSize = stationSize,
        symbol = symbol,
        color = ColorInt.from(color),
        closed = closed,
        stationList = stationList,
        polyline = polyline,
    )

    override suspend fun getLines(codes: List<Int>) =
        withContext(Dispatchers.IO) {
            dao.getLines(codes).map { it.toModel() }
        }

    override suspend fun getStation(code: Int) =
        withContext(Dispatchers.IO) {
            dao.getStation(code).toModel()
        }

    private suspend fun StationEntity.toModel() = Station(
        id = id,
        code = code,
        lat = lat,
        lng = lng,
        name = name,
        originalName = originalName,
        nameKana = nameKana,
        prefecture = prefectureRepository[prefecture],
        lines = getLines(lines),
        closed = closed,
        voronoi = voronoi,
    )

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

    override val dataVersion: Flow<DataVersionState>
        get() = dao.getCurrentDataVersion().map {
            if (it == null) {
                DataVersionState.None
            } else {
                DataVersionState.Initialized(it.toModel())
            }
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
        version
    }

    private fun File.stations() =
        json.decodeFromString<List<StationResponse>>(
            File(this, "json/station.json").readText(Charsets.UTF_8),
        ).map { it.toEntity() }

    private fun File.lines(): List<LineEntity> {
        val dir = File(this, "json/line")
        require(dir.exists() && dir.isDirectory)
        return requireNotNull(dir.listFiles()).map {
            val line = json.decodeFromString<LineResponse>(it.readText(Charsets.UTF_8))
            // load polyline from different file
            val file = File(this, "json/polyline/${line.code}.json")
            if (file.exists()) {
                line.copy(polyline = file.readText(Charsets.UTF_8))
            } else {
                line
            }
        }.map { it.toEntity() }
    }

    private fun File.kdTree() =
        json.decodeFromString<StationKdTree>(
            File(this, "json/tree.json").readText(Charsets.UTF_8),
        )
}

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface DataRepositoryModule {
    @Singleton
    @Binds
    fun bindDataRepository(impl: DataRepositoryImpl): DataRepository
}
