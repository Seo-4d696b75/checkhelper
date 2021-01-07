package jp.seo.station.ekisagasu.search

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * @author Seo-4d696b75
 * @version 2020/12/18.
 */
data class StationNode(
    @SerializedName("code")
    val code: Int,
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lng")
    val lng: Double,
    @SerializedName("left")
    val left: Int?,
    @SerializedName("right")
    val right: Int?,
    @SerializedName("segment")
    val segment: String?
)

@Entity(tableName = "tree")
data class TreeSegment(
    @PrimaryKey
    @ColumnInfo(name = "name", index = true)
    @SerializedName("name")
    val name: String,
    @ColumnInfo(name = "root")
    @SerializedName("root")
    val root: Int,
    @ColumnInfo(name = "station_size")
    @SerializedName("station_size")
    val size: Int,
    @ColumnInfo(name = "node_list")
    @SerializedName("node_list")
    val nodes: List<StationNode>
)
