package jp.seo.station.ekisagasu.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StationData(
    val version: Long,
    val stations: List<Station>,
    val lines: List<Line>,
    @SerialName("tree_segments")
    val trees: List<TreeSegment>
)
