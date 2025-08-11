package com.seo4d696b75.android.ekisagasu.domain.dataset

/**
 * @author Seo-4d696b75
 * @version 2020/12/17.
 */
data class Line(
    val id: Int,
    val code: Int,
    val name: String,
    val nameKana: String,
    val stationSize: Int,
    val symbol: String? = null,
    val color: ColorInt,
    val closed: Boolean,
    val stationList: List<StationRegistration>,
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
