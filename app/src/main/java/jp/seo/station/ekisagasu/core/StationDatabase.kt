package jp.seo.station.ekisagasu.core

import android.content.Context
import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.room.*
import jp.seo.station.ekisagasu.Line
import jp.seo.station.ekisagasu.Station
import jp.seo.station.ekisagasu.search.TreeSegment
import jp.seo.station.ekisagasu.core.StationRepository.UpdateProgressListener
import jp.seo.station.ekisagasu.utils.*
import java.util.*

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
@Database(entities = [Station::class, Line::class, TreeSegment::class, DataVersion::class], version = 4, exportSchema = false)
@TypeConverters(RegistrationListConverter::class, ArrayIntConverter::class, NodeListConverter::class, TimestampConverter::class)
abstract class StationDatabase : RoomDatabase(){
    abstract val dao: StationDao
}


private val holder = SingletonHolder<StationDatabase, Context> {
    Room.databaseBuilder(it, StationDatabase::class.java, "station_db")
        .fallbackToDestructiveMigration()
        .build()
}

fun getStationDatabase(ctx: Context) : StationDatabase = holder.get(ctx)

@Dao
abstract class StationDao {

    @Query("SELECT * FROM station WHERE code == :code")
    abstract fun getStation(code: Int): LiveData<Station>

    @Query("SELECT * FROM station WHERE id == :id")
    abstract fun getStation(id: String): LiveData<Station>

    @Query("SELECT * FROM station WHERE code IN (:codes) ORDER BY code")
    abstract suspend fun getStations(codes: List<Int>): List<Station>

    @Query("DELETE FROM station")
    abstract suspend fun clearStations()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun addStations(stations: List<Station>)

    @Query("SELECT * FROM line WHERE code == :code")
    abstract fun getLine(code: Int): LiveData<Line>

    @Query("SELECT * FROM line WHERE id == :id")
    abstract fun getLine(id: String): LiveData<Line>

    @Query("SELECT * FROM line WHERE code In (:codes) ORDER BY code")
    abstract suspend fun getLines(codes: Array<Int>): List<Line>

    @Query("DELETE FROM line")
    abstract suspend fun clearLines()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun addLines(lines: List<Line>)

    @Query("SELECT * FROM tree  WHERE name == :name")
    abstract suspend fun getTreeSegment(name: String): TreeSegment

    @Query("DELETE FROM tree")
    abstract suspend fun clearTreeSegments()

    @Insert(onConflict =  OnConflictStrategy.ABORT)
    abstract suspend fun addTreeSegments(trees: List<TreeSegment>)

    @Query("SELECT * FROM version_history")
    abstract suspend fun getDataVersionHistory(): List<DataVersion>

    @Query("SELECT * FROM version_history WHERE id == (SELECT MAX(id) FROM version_history)")
    abstract suspend fun getCurrentDataVersion(): DataVersion?

    @Insert
    abstract suspend fun setCurrentDataVersion(version: DataVersion)

    @Transaction
    open suspend fun updateData(data: StationData, listener: UpdateProgressListener, main: Handler){
        // abort and rollback if any error while this transaction, or data integrity lost!
        // delete old data
        main.post{
            listener.onStateChanged(UpdateProgressListener.STATE_CLEAN)
            listener.onProgress(0)
        }
        clearStations()
        main.post{ listener.onProgress(16) }
        clearLines()
        main.post{ listener.onProgress(33) }
        clearTreeSegments()
        main.post{
            listener.onStateChanged(UpdateProgressListener.STATE_ADD)
            listener.onProgress(50)
        }
        // add new data
        addStations(data.stations)
        main.post{ listener.onProgress(66) }
        addLines(data.lines)
        main.post{ listener.onProgress(83) }
        addTreeSegments(data.trees)
        main.post{ listener.onProgress(100) }
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
