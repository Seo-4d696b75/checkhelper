package jp.seo.station.ekisagasu.utils

import android.content.Context
import jp.seo.station.ekisagasu.R

// TODO context 拡張関数
fun formatTime(
    ctx: Context,
    sec: Int,
): String {
    return if (sec < 60) {
        sec.toString() + ctx.getString(R.string.time_unit_sec)
    } else if (sec < 3600) {
        (sec / 60).toString() + ctx.getString(R.string.time_unit_min)
    } else {
        (sec / 3600).toString() + ctx.getString(R.string.time_unit_hour)
    }
}
