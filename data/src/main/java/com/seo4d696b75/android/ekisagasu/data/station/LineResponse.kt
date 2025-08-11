package com.seo4d696b75.android.ekisagasu.data.station

import com.seo4d696b75.android.ekisagasu.data.database.station.LineEntity
import com.seo4d696b75.android.ekisagasu.domain.dataset.StationRegistration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class LineResponse(
    val id: Int,
    val code: Int,
    val name: String,
    @SerialName("name_kana")
    val nameKana: String,
    @SerialName("station_size")
    val stationSize: Int,
    val symbol: String? = null,
    val color: String? = null,
    val closed: Boolean,
    @SerialName("station_list")
    val stationList: List<StationRegistration>,
    @SerialName("polyline_list")
    @Serializable(with = JsonObjectAsStringSerializer::class)
    val polyline: String? = null,
) {
    fun toEntity() = LineEntity(id, code, name, nameKana, stationSize, symbol, color, closed, stationList, polyline)
}
