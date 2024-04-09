package com.seo4d696b75.android.ekisagasu.data.station

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author Seo-4d696b75
 * @version 2020/12/17.
 */
@Serializable
@Entity(tableName = "line", indices = [Index(value = ["id", "code"], unique = true)])
data class Line(
    @ColumnInfo(name = "id")
    val id: String,
    @PrimaryKey
    @ColumnInfo(name = "code")
    val code: Int,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "name_kana")
    @SerialName("name_kana")
    val nameKana: String,
    @ColumnInfo(name = "station_size")
    @SerialName("station_size")
    val stationSize: Int,
    @ColumnInfo(name = "symbol")
    val symbol: String? = null,
    @ColumnInfo(name = "color")
    val color: String? = null,
    @ColumnInfo(name = "closed")
    val closed: Boolean,
    @ColumnInfo(name = "station_list")
    @SerialName("station_list")
    val stationList: Array<StationRegistration>,
    @SerialName("polyline_list")
    @Serializable(with = JsonObjectAsStringSerializer::class)
    @ColumnInfo(name = "polyline")
    val polyline: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Line

        if (id != other.id) return false
        if (code != other.code) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + code
        return result
    }
}
