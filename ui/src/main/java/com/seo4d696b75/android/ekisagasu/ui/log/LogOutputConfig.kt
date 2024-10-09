package com.seo4d696b75.android.ekisagasu.ui.log

import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class LogOutputExtension {
    TXT,
    GPX,
}

@Serializable
sealed interface LogOutputConfig {
    val filter: AppLogType.Filter
    val extension: LogOutputExtension

    @SerialName("all")
    data object All : LogOutputConfig {
        override val filter = AppLogType.Filter.All
        override val extension = LogOutputExtension.TXT
    }

    @SerialName("system")
    data object System : LogOutputConfig {
        override val filter = AppLogType.Filter.System
        override val extension = LogOutputExtension.TXT
    }

    @SerialName("geo")
    data class Geo(override val extension: LogOutputExtension) : LogOutputConfig {
        override val filter = AppLogType.Filter.Geo
    }

    @SerialName("station")
    data object Station : LogOutputConfig {
        override val filter = AppLogType.Filter.Station
        override val extension = LogOutputExtension.TXT
    }
}
