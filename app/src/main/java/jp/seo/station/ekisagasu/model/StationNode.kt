package jp.seo.station.ekisagasu.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author Seo-4d696b75
 * @version 2020/12/18.
 */
@Serializable
data class StationNode(
    val code: Int,
    val lat: Double?,
    val lng: Double?,
    val left: Int?,
    val right: Int?,
    val segment: String?
)

@Serializable
@Entity(tableName = "tree")
data class TreeSegment(
    @PrimaryKey
    @ColumnInfo(name = "name", index = true)
    val name: String,
    @ColumnInfo(name = "root")
    val root: Int,
    @ColumnInfo(name = "station_size")
    @SerialName("station_size")
    val size: Int,
    @ColumnInfo(name = "node_list")
    @SerialName("node_list")
    val nodes: List<StationNode>
)
