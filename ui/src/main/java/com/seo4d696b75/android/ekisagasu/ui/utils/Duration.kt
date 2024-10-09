package com.seo4d696b75.android.ekisagasu.ui.utils

import android.content.Context
import com.seo4d696b75.android.ekisagasu.ui.R

fun Context.formatDuration(sec: Int): String = if (sec < 60) {
    sec.toString() + getString(R.string.time_unit_sec)
} else if (sec < 3600) {
    (sec / 60).toString() + getString(R.string.time_unit_min)
} else {
    (sec / 3600).toString() + getString(R.string.time_unit_hour)
}
