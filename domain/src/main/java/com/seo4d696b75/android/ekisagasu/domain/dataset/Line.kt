package com.seo4d696b75.android.ekisagasu.domain.dataset

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author Seo-4d696b75
 * @version 2020/12/17.
 */
@Serializable
data class Line(
    val id: String,
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
    val stationList: Array<StationRegistration>,
    @SerialName("polyline_list")
    @Serializable(with = JsonObjectAsStringSerializer::class)
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
