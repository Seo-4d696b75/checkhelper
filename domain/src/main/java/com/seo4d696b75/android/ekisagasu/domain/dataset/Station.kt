package com.seo4d696b75.android.ekisagasu.domain.dataset

/**
 * @author Seo-4d696b75
 * @version 2020/12/17.
 */
data class Station(
    val id: Int,
    val code: Int,
    val lat: Double,
    val lng: Double,
    val name: String,
    val originalName: String,
    val nameKana: String,
    val prefecture: Prefecture,
    val lines: List<Line>,
    val closed: Boolean,
    val voronoi: String,
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
        return lines.contains(line)
    }

    fun getLinesName(): String = lines.joinToString(separator = " ", transform = { line -> line.name })
}
