package jp.seo.station.ekisagasu.database

import androidx.room.*
import jp.seo.station.ekisagasu.model.Line
import jp.seo.station.ekisagasu.model.Station
import jp.seo.station.ekisagasu.model.StationData
import jp.seo.station.ekisagasu.model.TreeSegment
import java.util.*

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
@Database(
    entities = [Station::class, Line::class, TreeSegment::class, DataVersion::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(
    RegistrationListConverter::class,
    ArrayIntConverter::class,
    IntListConverter::class,
    NodeListConverter::class,
    TimestampConverter::class
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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun addStations(stations: List<Station>)

    @Query("SELECT * FROM line WHERE code == :code")
    abstract suspend fun getLine(code: Int): Line

    @Query("SELECT * FROM line WHERE code In (:codes) ORDER BY code")
    abstract suspend fun getLines(codes: List<Int>): List<Line>

    @Query("DELETE FROM line")
    abstract suspend fun clearLines()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun addLines(lines: List<Line>)

    @Query("SELECT * FROM tree  WHERE name == :name")
    abstract suspend fun getTreeSegment(name: String): TreeSegment

    @Query("DELETE FROM tree")
    abstract suspend fun clearTreeSegments()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun addTreeSegments(trees: List<TreeSegment>)

    @Query("SELECT * FROM version_history")
    abstract suspend fun getDataVersionHistory(): List<DataVersion>

    @Query("SELECT * FROM version_history WHERE id == (SELECT MAX(id) FROM version_history)")
    abstract suspend fun getCurrentDataVersion(): DataVersion?

    @Insert
    abstract suspend fun setCurrentDataVersion(version: DataVersion)

    @Transaction
    open suspend fun updateData(data: StationData) {
        // abort and rollback if any error while this transaction, or data integrity lost!
        // delete old data
        clearStations()
        clearLines()

        clearTreeSegments()

        // add new data
        addStations(data.stations)
        addLines(data.lines)
        addTreeSegments(data.trees)

        // update version
        setCurrentDataVersion(DataVersion(data.version))
    }
}

@Entity(tableName = "version_history")
data class DataVersion(
    @ColumnInfo(name = "version")
    val version: Long
) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    var id: Long = 0

    @ColumnInfo(name = "timestamp")
    var timestamp: Date = Date()
}
