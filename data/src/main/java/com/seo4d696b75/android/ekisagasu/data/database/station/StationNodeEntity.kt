package com.seo4d696b75.android.ekisagasu.data.database.station

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.seo4d696b75.android.ekisagasu.domain.kdtree.StationNode

@Entity(tableName = "node")
data class StationNodeEntity(
    @PrimaryKey
    val code: Int,
    val lat: Double,
    val lng: Double,
    val left: Int?,
    val right: Int?,
) {
    fun toModel() = StationNode(code, lat, lng, left, right)

    companion object {
        fun fromModel(m: StationNode) = m.run {
            StationNodeEntity(code, lat, lng, left, right)
        }
    }
}
