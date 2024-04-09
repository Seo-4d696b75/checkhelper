package com.seo4d696b75.android.ekisagasu.data.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.seo4d696b75.android.ekisagasu.data.log.AppLogType
import com.seo4d696b75.android.ekisagasu.data.utils.TIME_PATTERN_MILLI_SEC
import com.seo4d696b75.android.ekisagasu.data.utils.formatTime
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
@Database(entities = [AppLog::class, AppRebootLog::class], version = 2, exportSchema = false)
@TypeConverters(TimestampConverter::class, AppLogTypeConverter::class)
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

class AppLogTypeConverter {
    @TypeConverter
    fun fromAppLogType(type: AppLogType?): Int? = type?.value

    @TypeConverter
    fun toAppLogType(value: Int?): AppLogType? = AppLogType.entries.firstOrNull { it.value == value }
}

@Entity(tableName = "log")
data class AppLog constructor(
    @ColumnInfo(name = "type")
    val type: AppLogType,
    @ColumnInfo(name = "message")
    val message: String,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    var id: Long = 0

    @ColumnInfo(name = "timestamp")
    var timestamp: Date = Date()

    override fun toString(): String {
        return String.format(
            "%s %s",
            formatTime(TIME_PATTERN_MILLI_SEC, timestamp),
            message,
        )
    }
}

@Entity(
    tableName = "reboot",
    foreignKeys = [
        ForeignKey(
            entity = AppLog::class,
            parentColumns = ["id"],
            childColumns = ["id"],
        ),
    ],
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
    val error: Boolean = false,
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
    abstract suspend fun writeFinish(
        finish: Date,
        error: Boolean,
    )

    @Query("SELECT MAX(id) FROM log")
    abstract suspend fun getLatestID(): Long

    @Insert
    abstract suspend fun insertRebootLog(log: AppRebootLog)

    @Query("SELECT * FROM reboot WHERE id = (SELECT MAX(id) FROM reboot)")
    abstract suspend fun getCurrentReboot(): AppRebootLog

    @Query("SELECT MIN(id) FROM reboot WHERE id > :id")
    abstract suspend fun getNextReboot(id: Long): Long?

    @Query("SELECT * FROM reboot ORDER BY id DESC")
    abstract fun getRebootHistory(): Flow<List<AppRebootLog>>

    @Query("SELECT * FROM log WHERE :sinceID <= id AND id < :untilID")
    abstract fun getLogs(
        sinceID: Long,
        untilID: Long = Long.MAX_VALUE,
    ): Flow<List<AppLog>>

    @Query("SELECT * FROM log WHERE :sinceID <= id AND id < :untilID")
    abstract fun getLogsOneshot(
        sinceID: Long,
        untilID: Long = Long.MAX_VALUE,
    ): List<AppLog>
}
