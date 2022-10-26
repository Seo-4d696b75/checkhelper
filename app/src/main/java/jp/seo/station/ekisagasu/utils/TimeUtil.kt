package jp.seo.station.ekisagasu.utils

import android.content.Context
import jp.seo.station.ekisagasu.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * @author Seo-4d696b75
 * @version 2021/02/04.
 */

const val TIME_PATTERN_SIMPLE = "HH:mm"
const val TIME_PATTERN_MILLI_SEC = "HH:mm:ss.SSS"
const val TIME_PATTERN_ISO8601_EXTEND = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
const val TIME_PATTERN_DATETIME = "yyyy-MM-dd HH:mm"
const val TIME_PATTERN_DATETIME_FILE = "yyyyMMdd_HHmm"

fun formatTime(pattern: String, time: Date?): String {
    return time?.let {
        SimpleDateFormat(pattern, Locale.US).format(it)
    } ?: ""
}

fun formatTime(ctx: Context, sec: Int): String {
    return if (sec < 60) {
        sec.toString() + ctx.getString(R.string.time_unit_sec)
    } else if (sec < 3600) {
        (sec / 60).toString() + ctx.getString(R.string.time_unit_min)
    } else {
        (sec / 3600).toString() + ctx.getString(R.string.time_unit_hour)
    }
}
