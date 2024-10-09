package com.seo4d696b75.android.ekisagasu.domain.log

import java.util.Date

data class AppLogTarget(
    val id: Long,
    val range: LongRange,
    val start: Date,
    val end: Date?,
    val hasError: Boolean,
)
