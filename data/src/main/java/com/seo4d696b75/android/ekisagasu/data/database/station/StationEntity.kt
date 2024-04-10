package com.seo4d696b75.android.ekisagasu.data.database.station

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station

@Entity(tableName = "station", indices = [Index(value = ["id", "code"], unique = true)])
data class StationEntity(
    @ColumnInfo(name = "id")
    val id: String,
    @PrimaryKey
    @ColumnInfo(name = "code")
    val code: Int,
    @ColumnInfo(name = "lat")
    val lat: Double,
    @ColumnInfo(name = "lng")
    val lng: Double,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "original_name")
    val originalName: String,
    @ColumnInfo(name = "name_kana")
    val nameKana: String,
    @ColumnInfo(name = "prefecture")
    val prefecture: Int,
    @ColumnInfo(name = "lines")
    val lines: List<Int>,
    @ColumnInfo(name = "closed")
    val closed: Boolean,
    @ColumnInfo(name = "voronoi")
    val voronoi: String,
    @ColumnInfo(name = "attr")
    val attr: String?,
) {
    fun toModel() = Station(id, code, lat, lng, name, originalName, nameKana, prefecture, lines, closed, voronoi, attr)

    companion object {
        fun fromModel(s: Station) = s.run {
            StationEntity(id, code, lat, lng, name, originalName, nameKana, prefecture, lines, closed, voronoi, attr)
        }
    }
}
