package jp.seo.station.ekisagasu.model

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
@Entity(tableName = "station", indices = [Index(value = ["id", "code"], unique = true)])
data class Station constructor(
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
    @SerialName("original_name")
    val originalName: String,
    @ColumnInfo(name = "name_kana")
    @SerialName("name_kana")
    val nameKana: String,
    @ColumnInfo(name = "prefecture")
    val prefecture: Int,
    @ColumnInfo(name = "lines")
    val lines: Array<Int>,
    @ColumnInfo(name = "closed")
    val closed: Boolean,
    @ColumnInfo(name = "next")
    val next: Array<Int>,
    @ColumnInfo(name = "voronoi")
    val voronoi: String,
    @ColumnInfo(name = "attr")
    val attr: String?
) {


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Station

        if (id != other.id) return false
        if (code != other.code) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + code
        return result
    }

    fun isLine(line: Line): Boolean {
        return lines.contains(line.code)
    }

}

@Serializable
data class StationRegistration(
    val code: Int,
    val numbering: List<String>?
) {

    fun getNumberingString(): String {
        return numbering?.joinToString(separator = "/", transform = String::toString) ?: ""
    }

}
