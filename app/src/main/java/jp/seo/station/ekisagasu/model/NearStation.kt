package jp.seo.station.ekisagasu.model

import jp.seo.station.ekisagasu.utils.TIME_PATTERN_SIMPLE
import jp.seo.station.ekisagasu.utils.formatTime
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
