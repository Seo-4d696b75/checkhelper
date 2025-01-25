package com.seo4d696b75.android.ekisagasu.ui.log

import com.seo4d696b75.android.ekisagasu.domain.config.AppConfig
import com.seo4d696b75.android.ekisagasu.domain.date.TIME_PATTERN_DATETIME
import com.seo4d696b75.android.ekisagasu.domain.date.TIME_PATTERN_MILLI_SEC
import com.seo4d696b75.android.ekisagasu.domain.date.format
import com.seo4d696b75.android.ekisagasu.domain.log.AppLog
import java.io.OutputStream
import java.util.Date
import javax.inject.Inject

class LogSerializer @Inject constructor(
    private val appConfig: AppConfig,
) {
    operator fun invoke(
        config: LogOutputConfig,
        logs: List<AppLog>,
        dst: OutputStream,
    ) {
        dst.bufferedWriter().use { writer ->
            writer.apply {
                append(appConfig.appName)
                append("\nlog type : ")
                append(config.filter.name)
                append("\nwritten time : ")
                append(Date(config.timestamp).format(TIME_PATTERN_DATETIME))
                for (log in logs) {
                    append("\n")
                    append(log.timestamp.format(TIME_PATTERN_MILLI_SEC))
                    append(" ")
                    append(log.message)
                }
            }
        }
    }
}
