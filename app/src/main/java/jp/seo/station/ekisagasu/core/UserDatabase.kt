package jp.seo.station.ekisagasu.core

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*
import jp.seo.station.ekisagasu.utils.SingletonHolder
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
@Database(entities = [AppLog::class, AppRebootLog::class], version = 1, exportSchema = false)
@TypeConverters(TimestampConverter::class)
abstract class UserDatabase : RoomDatabase() {
    abstract val userDao: UserDao
}

private val holder = SingletonHolder<UserDatabase, Context> {
    Room.databaseBuilder(it, UserDatabase::class.java, "user_db")
        .fallbackToDestructiveMigration()
        .build()
}

fun getUserDatabase(ctx: Context) : UserDatabase = holder.get(ctx)

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
            SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(timestamp),
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
    @ColumnInfo(name = "timestamp")
    val timestamp: Date
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

    @Query("SELECT MAX(id) FROM log")
    abstract suspend fun getLatestID(): Long

    @Insert
    abstract suspend fun insertRebootLog(log: AppRebootLog)

    @Query("SELECT MAX(id) FROM reboot")
    abstract suspend fun getCurrentReboot(): Long

    @Query("SELECT * FROM log WHERE id >= :sinceID")
    abstract fun getLogs(sinceID: Long): LiveData<List<AppLog>>

}
