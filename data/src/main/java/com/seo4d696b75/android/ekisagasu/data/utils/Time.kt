package com.seo4d696b75.android.ekisagasu.data.utils

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

// TODO date 拡張関数
fun formatTime(
    pattern: String,
    time: Date?,
): String = time?.let {
    SimpleDateFormat(pattern, Locale.US).format(it)
} ?: ""
