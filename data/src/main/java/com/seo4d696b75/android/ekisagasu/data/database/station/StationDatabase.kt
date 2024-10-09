package com.seo4d696b75.android.ekisagasu.data.database.station

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import com.seo4d696b75.android.ekisagasu.data.database.user.TimestampConverter
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataVersion
import com.seo4d696b75.android.ekisagasu.domain.kdtree.StationKdTree

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
@Database(
    entities = [
        StationEntity::class,
        LineEntity::class,
        StationNodeEntity::class,
        RootStationNodeEntity::class,
        DataVersionEntity::class,
    ],
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
    abstract suspend fun getStation(code: Int): StationEntity

    @Query("SELECT * FROM station WHERE code IN (:codes) ORDER BY code")
    abstract suspend fun getStations(codes: List<Int>): List<StationEntity>

    @Query("DELETE FROM station")
    abstract suspend fun clearStations()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addStations(stations: List<StationEntity>)

    @Query("SELECT * FROM line WHERE code == :code")
    abstract suspend fun getLine(code: Int): LineEntity

    @Query("SELECT * FROM line WHERE code In (:codes) ORDER BY code")
    abstract suspend fun getLines(codes: List<Int>): List<LineEntity>

    @Query("DELETE FROM line")
    abstract suspend fun clearLines()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addLines(lines: List<LineEntity>)

    @Query("SELECT * FROM node")
    abstract suspend fun getStationNodes(): List<StationNodeEntity>

    @Query("DELETE FROM node")
    abstract suspend fun clearStationNodes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addStationNodes(nodes: List<StationNodeEntity>)

    @Query("SELECT * FROM root_node LIMIT 1")
    abstract suspend fun getRootStationNode(): RootStationNodeEntity

    @Query("DELETE FROM root_node")
    abstract suspend fun clearRootStationNode()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addRootStationNode(root: RootStationNodeEntity)

    @Query("SELECT * FROM version_history")
    abstract suspend fun getDataVersionHistory(): List<DataVersionEntity>

    @Query("SELECT * FROM version_history WHERE id == (SELECT MAX(id) FROM version_history)")
    abstract suspend fun getCurrentDataVersion(): DataVersionEntity?

    @Insert
    abstract suspend fun setCurrentDataVersion(version: DataVersionEntity)

    @Transaction
    open suspend fun updateData(
        version: Long,
        stations: List<StationEntity>,
        lines: List<LineEntity>,
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
        addStationNodes(tree.nodes.map { StationNodeEntity.fromModel(it) })
        addRootStationNode(RootStationNodeEntity(tree.root))

        // update version
        val v = DataVersionEntity(version)
        setCurrentDataVersion(v)
        return v.toModel()
    }
}
