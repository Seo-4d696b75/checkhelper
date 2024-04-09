package com.seo4d696b75.android.ekisagasu.data.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.model.RootStationNode
import jp.seo.station.ekisagasu.model.Station
import jp.seo.station.ekisagasu.model.StationKdTree
import jp.seo.station.ekisagasu.model.StationNode
import java.util.Date

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
@Database(
    entities = [Station::class, Line::class, StationNode::class, RootStationNode::class, DataVersion::class],
    version = 8,
    exportSchema = false,
)
@TypeConverters(
    RegistrationListConverter::class,
    ArrayIntConverter::class,
    IntListConverter::class,
    NodeListConverter::class,
    TimestampConverter::class,
)
abstract class StationDatabase : RoomDatabase() {
    abstract val dao: StationDao
}

@Dao
abstract class StationDao {
    @Query("SELECT * FROM station WHERE code == :code")
    abstract suspend fun getStation(code: Int): Station

    @Query("SELECT * FROM station WHERE code IN (:codes) ORDER BY code")
    abstract suspend fun getStations(codes: List<Int>): List<Station>

    @Query("DELETE FROM station")
    abstract suspend fun clearStations()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addStations(stations: List<Station>)

    @Query("SELECT * FROM line WHERE code == :code")
    abstract suspend fun getLine(code: Int): Line

    @Query("SELECT * FROM line WHERE code In (:codes) ORDER BY code")
    abstract suspend fun getLines(codes: List<Int>): List<Line>

    @Query("DELETE FROM line")
    abstract suspend fun clearLines()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addLines(lines: List<Line>)

    @Query("SELECT * FROM node")
    abstract suspend fun getStationNodes(): List<StationNode>

    @Query("DELETE FROM node")
    abstract suspend fun clearStationNodes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addStationNodes(nodes: List<StationNode>)

    @Query("SELECT * FROM root_node LIMIT 1")
    abstract suspend fun getRootStationNode(): RootStationNode

    @Query("DELETE FROM root_node")
    abstract suspend fun clearRootStationNode()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addRootStationNode(root: RootStationNode)

    @Query("SELECT * FROM version_history")
    abstract suspend fun getDataVersionHistory(): List<DataVersion>

    @Query("SELECT * FROM version_history WHERE id == (SELECT MAX(id) FROM version_history)")
    abstract suspend fun getCurrentDataVersion(): DataVersion?

    @Insert
    abstract suspend fun setCurrentDataVersion(version: DataVersion)

    @Transaction
    open suspend fun updateData(
        version: Long,
        stations: List<Station>,
        lines: List<Line>,
        tree: StationKdTree,
    ): DataVersion {
        // abort and rollback if any error while this transaction, or data integrity lost!
        // delete old data
        clearStations()
        clearLines()
        clearStationNodes()
        clearRootStationNode()

        // add new data
        addStations(stations)
        addLines(lines)
        addStationNodes(tree.nodes)
        addRootStationNode(RootStationNode(tree.root))

        // update version
        return DataVersion(version).also {
            setCurrentDataVersion(it)
        }
    }
}

@Entity(tableName = "version_history")
data class DataVersion(
    @ColumnInfo(name = "version")
    val version: Long,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    var id: Long = 0

    @ColumnInfo(name = "timestamp")
    var timestamp: Date = Date()
}
