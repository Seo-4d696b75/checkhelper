package com.seo4d696b75.android.ekisagasu.data.database.user

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
@Database(entities = [AppLogEntity::class, AppRebootEntity::class], version = 2, exportSchema = false)
@TypeConverters(TimestampConverter::class, AppLogTypeConverter::class)
abstract class UserDatabase : RoomDatabase() {
    abstract val userDao: UserDao
}

@Dao
abstract class UserDao {
    @Insert
    abstract suspend fun insertLog(log: AppLogEntity)

    @Transaction
    open suspend fun insertRebootLog(log: AppLogEntity) {
        insertLog(log)
        val id = getLatestID()
        insertRebootLog(AppRebootEntity(id, log.timestamp))
    }

    @Query("UPDATE reboot SET finish = :finish, has_error = :error WHERE id = (SELECT MAX(id) FROM reboot)")
    abstract suspend fun writeFinish(
        finish: Date,
        error: Boolean,
    )

    @Query("SELECT MAX(id) FROM log")
    abstract suspend fun getLatestID(): Long

    @Insert
    abstract suspend fun insertRebootLog(log: AppRebootEntity)

    @Query("SELECT * FROM reboot WHERE id = (SELECT MAX(id) FROM reboot)")
    abstract suspend fun getCurrentReboot(): AppRebootEntity

    @Query("SELECT MIN(id) FROM reboot WHERE id > :id")
    abstract suspend fun getNextReboot(id: Long): Long?

    @Query("SELECT * FROM reboot ORDER BY id DESC")
    abstract fun getRebootHistory(): Flow<List<AppRebootEntity>>

    @Query("SELECT * FROM log WHERE :sinceID <= id AND id < :untilID")
    abstract fun getLogs(
        sinceID: Long,
        untilID: Long = Long.MAX_VALUE,
    ): Flow<List<AppLogEntity>>

    @Query("SELECT * FROM log WHERE :sinceID <= id AND id < :untilID")
    abstract fun getLogsOneshot(
        sinceID: Long,
        untilID: Long = Long.MAX_VALUE,
    ): List<AppLogEntity>
}
