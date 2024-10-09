package com.seo4d696b75.android.ekisagasu.domain.log

import com.seo4d696b75.android.ekisagasu.domain.date.TIME_PATTERN_MILLI_SEC
import com.seo4d696b75.android.ekisagasu.domain.date.format
import java.util.Date

data class AppLog(
    val id: Long,
    val type: AppLogType,
    val message: String,
    val timestamp: Date,
) {
    override fun toString(): String = String.format(
        "%s %s",
        timestamp.format(TIME_PATTERN_MILLI_SEC),
        message,
    )
}

fun List<AppLog>.filter(filter: AppLogType.Filter) =
    filter { it.type.value and filter.value > 0 }
