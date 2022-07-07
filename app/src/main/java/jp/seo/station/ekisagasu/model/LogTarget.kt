package jp.seo.station.ekisagasu.model

import jp.seo.station.ekisagasu.core.AppRebootLog

data class LogTarget(
    val target: AppRebootLog?,
    val since: Long,
    val until: Long = Long.MAX_VALUE
)