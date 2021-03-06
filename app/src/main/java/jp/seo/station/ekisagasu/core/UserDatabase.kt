package jp.seo.station.ekisagasu.core

import androidx.lifecycle.LiveData
import androidx.room.*
import jp.seo.station.ekisagasu.utils.TIME_PATTERN_MILLI_SEC
import jp.seo.station.ekisagasu.utils.formatTime
import java.util.*

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
@Database(entities = [AppLog::class, AppRebootLog::class], version = 2, exportSchema = false)
@TypeConverters(TimestampConverter::class)
abstract class UserDatabase : RoomDatabase() {
    abstract val userDao: UserDao
}

class TimestampConverter {

    @TypeConverter
    fun convertDatetime(value: Date?): Long? {
        return value?.time
    }

    @TypeConverter
    fun convertUnixTime(value: Long?): Date? {
        return value?.let {
            Date(it)
        }
    }
}

@Entity(tableName = "log")
data class AppLog constructor(
    @ColumnInfo(name = "type")
    val type: Int,
    @ColumnInfo(name = "message")
    val message: String
) {


    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    var id: Long = 0

    @ColumnInfo(name = "timestamp")
    var timestamp: Date = Date()

    companion object {
        val TYPE_SYSTEM = 0b001
        val TYPE_LOCATION = 0b010
        val TYPE_STATION = 0b100

        val FILTER_ALL = 0b111
        val FILTER_GEO = 0b110

    }

    override fun toString(): String {
        return String.format(
            "%s %s",
            formatTime(TIME_PATTERN_MILLI_SEC, timestamp),
            message
        )
    }
}

@Entity(
    tableName = "reboot", foreignKeys = [
        ForeignKey(
            entity = AppLog::class,
            parentColumns = ["id"],
            childColumns = ["id"]
        )
    ]
)
data class AppRebootLog constructor(
    @PrimaryKey
    @ColumnInfo(name = "id", index = true)
    val id: Long,
    @ColumnInfo(name = "start")
    val start: Date,
    @ColumnInfo(name = "finish")
    val finish: Date? = null,
    @ColumnInfo(name = "has_error")
    val error: Boolean = false
)

@Dao
abstract class UserDao {

    @Insert
    abstract suspend fun insertLog(log: AppLog)

    @Transaction
    open suspend fun insertRebootLog(log: AppLog) {
        insertLog(log)
        val id = getLatestID()
        insertRebootLog(AppRebootLog(id, log.timestamp))
    }

    @Query("UPDATE reboot SET finish = :finish, has_error = :error WHERE id = (SELECT MAX(id) FROM reboot)")
    abstract suspend fun writeFinish(finish: Date, error: Boolean)

    @Query("SELECT MAX(id) FROM log")
    abstract suspend fun getLatestID(): Long

    @Insert
    abstract suspend fun insertRebootLog(log: AppRebootLog)

    @Query("SELECT * FROM reboot WHERE id = (SELECT MAX(id) FROM reboot)")
    abstract suspend fun getCurrentReboot(): AppRebootLog

    @Query("SELECT MIN(id) FROM reboot WHERE id > :id")
    abstract suspend fun getNextReboot(id: Long): Long?

    @Query("SELECT * FROM reboot ORDER BY id DESC")
    abstract fun getRebootHistory(): LiveData<List<AppRebootLog>>

    @Query("SELECT * FROM log WHERE :sinceID <= id AND id < :untilID")
    abstract fun getLogs(sinceID: Long, untilID: Long = Long.MAX_VALUE): LiveData<List<AppLog>>

}
