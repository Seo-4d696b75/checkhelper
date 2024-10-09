package com.seo4d696b75.android.ekisagasu.data.database.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "reboot",
    foreignKeys = [
        ForeignKey(
            entity = AppLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
        ),
    ],
)
data class AppRebootEntity(
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
