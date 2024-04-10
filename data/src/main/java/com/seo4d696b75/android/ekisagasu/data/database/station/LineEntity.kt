package com.seo4d696b75.android.ekisagasu.data.database.station

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.StationRegistration

@Entity(tableName = "line", indices = [Index(value = ["id", "code"], unique = true)])
data class LineEntity(
    @ColumnInfo(name = "id")
    val id: String,
    @PrimaryKey
    @ColumnInfo(name = "code")
    val code: Int,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "name_kana")
    val nameKana: String,
    @ColumnInfo(name = "station_size")
    val stationSize: Int,
    @ColumnInfo(name = "symbol")
    val symbol: String?,
    @ColumnInfo(name = "color")
    val color: String?,
    @ColumnInfo(name = "closed")
    val closed: Boolean,
    @ColumnInfo(name = "station_list")
    val stationList: Array<StationRegistration>,
    @ColumnInfo(name = "polyline")
    val polyline: String?,
) {
    fun toModel() = Line(id, code, name, nameKana, stationSize, symbol, color, closed, stationList, polyline)

    companion object {
        fun fromModel(l: Line) = l.run {
            LineEntity(id, code, name, nameKana, stationSize, symbol, color, closed, stationList, polyline)
        }
    }
}
