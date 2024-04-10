package com.seo4d696b75.android.ekisagasu.domain.log

import java.util.Date

data class AppLogTarget(
    // TODO dbのidがdata層に隠蔽されていない
    val id: LongRange,
    val start: Date,
    val end: Date?,
    val hasError: Boolean,
)
