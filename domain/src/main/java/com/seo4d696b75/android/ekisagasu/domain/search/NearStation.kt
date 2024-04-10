package com.seo4d696b75.android.ekisagasu.domain.search

import com.seo4d696b75.android.ekisagasu.domain.dataset.Line
import com.seo4d696b75.android.ekisagasu.domain.dataset.Station
import com.seo4d696b75.android.ekisagasu.domain.date.TIME_PATTERN_SIMPLE
import com.seo4d696b75.android.ekisagasu.domain.date.format
import java.util.Date

data class NearStation(
    val station: Station,
    /**
     * distance from the current position to this station
     */
    val distance: Float,
    /**
     * Time when this near station detected
     */
    val time: Date,
    val lines: List<Line>,
) {
    fun getDetectedTime(): String = time.format(TIME_PATTERN_SIMPLE)

    fun getLinesName(): String = lines.joinToString(separator = " ", transform = { line -> line.name })
}
