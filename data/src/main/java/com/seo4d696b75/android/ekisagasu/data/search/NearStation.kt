package com.seo4d696b75.android.ekisagasu.data.search

import com.seo4d696b75.android.ekisagasu.data.station.Line
import com.seo4d696b75.android.ekisagasu.data.station.Station
import com.seo4d696b75.android.ekisagasu.data.utils.TIME_PATTERN_SIMPLE
import com.seo4d696b75.android.ekisagasu.data.utils.formatTime
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
    fun getDetectedTime(): String {
        return formatTime(TIME_PATTERN_SIMPLE, time)
    }

    fun getLinesName(): String {
        return lines.joinToString(separator = " ", transform = { line -> line.name })
    }
}
