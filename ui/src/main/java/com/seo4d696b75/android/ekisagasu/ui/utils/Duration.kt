package com.seo4d696b75.android.ekisagasu.ui.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogTarget
import com.seo4d696b75.android.ekisagasu.ui.R

fun Context.formatDuration(sec: Int): String = if (sec < 60) {
    sec.toString() + getString(R.string.time_unit_sec)
} else if (sec < 3600) {
    (sec / 60).toString() + getString(R.string.time_unit_min)
} else {
    (sec / 3600).toString() + getString(R.string.time_unit_hour)
}

@Composable
fun AppLogTarget.duration(): String = end?.let {
    val sec = ((it.time - start.time) / 1000L).toInt()
    LocalContext.current.formatDuration(sec)
} ?: ""
