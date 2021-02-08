package jp.seo.station.ekisagasu.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Seo-4d696b75
 * @version 2021/02/04.
 */

const val TIME_PATTERN_SIMPLE = "HH:mm"
const val TIME_PATTERN_MILLI_SEC = "HH:mm:ss.SSS"
const val TIME_PATTERN_ISO8601_EXTEND = "yyyy-MM-dd'T'HH:mm:ssZ"
const val TIME_PATTERN_DATETIME = "yyyy-MM-dd HH:mm"
const val TIME_PATTERN_DATETIME_FILE = "yyyyMMdd_HHmm"

fun formatTime(pattern: String, time: Date?): String {
    return time?.let {
        SimpleDateFormat(pattern, Locale.US).format(it)
    } ?: ""
}
