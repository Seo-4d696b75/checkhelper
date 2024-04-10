package com.seo4d696b75.android.ekisagasu.domain.kdtree

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author Seo-4d696b75
 * @version 2020/12/18.
 */
@Serializable
data class StationNode(
    val code: Int,
    val lat: Double,
    val lng: Double,
    val left: Int? = null,
    val right: Int? = null,
)

@Serializable
data class StationKdTree(
    val root: Int,
    @SerialName("node_list")
    val nodes: List<StationNode>,
)
