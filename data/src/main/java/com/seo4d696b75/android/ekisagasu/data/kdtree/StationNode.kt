package com.seo4d696b75.android.ekisagasu.data.kdtree

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author Seo-4d696b75
 * @version 2020/12/18.
 */
@Serializable
@Entity(tableName = "node")
data class StationNode(
    @PrimaryKey
    val code: Int,
    val lat: Double,
    val lng: Double,
    val left: Int? = null,
    val right: Int? = null,
)

@Serializable
@Entity(tableName = "root_node")
data class RootStationNode(
    @PrimaryKey
    val code: Int,
)

@Serializable
data class StationKdTree(
    val root: Int,
    @SerialName("node_list")
    val nodes: List<StationNode>,
)
