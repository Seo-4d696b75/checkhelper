package com.seo4d696b75.android.ekisagasu.data.database.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.seo4d696b75.android.ekisagasu.domain.log.AppLog
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
import java.util.Date

@Entity(tableName = "log")
data class AppLogEntity(
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

    fun toModel() = AppLog(id, type, message, timestamp)
}
