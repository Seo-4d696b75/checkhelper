package com.seo4d696b75.android.ekisagasu.data.database.station

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataVersion
import java.util.Date

@Entity(tableName = "version_history")
data class DataVersionEntity(
    @ColumnInfo(name = "version")
    val version: Long,
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    var id: Long = 0

    @ColumnInfo(name = "timestamp")
    var timestamp: Date = Date()

    fun toModel() = DataVersion(version, timestamp)
}
