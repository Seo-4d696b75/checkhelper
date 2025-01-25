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
    val fileBaseName: String
    val extension: LogOutputExtension
    val timestamp: Long

    val fineName: String
        get() = when (extension) {
            LogOutputExtension.TXT -> "$fileBaseName.txt"
            LogOutputExtension.GPX -> "$fileBaseName.gpx"
        }

    @SerialName("all")
    data class All(
        override val timestamp: Long,
        override val fileBaseName: String,
    ) : LogOutputConfig {
        override val filter = AppLogType.Filter.All
        override val extension = LogOutputExtension.TXT
    }

    @SerialName("system")
    data class System(
        override val timestamp: Long,
        override val fileBaseName: String,
    ) : LogOutputConfig {
        override val filter = AppLogType.Filter.System
        override val extension = LogOutputExtension.TXT
    }

    @SerialName("geo")
    @Serializable
    data class Geo(
        override val timestamp: Long,
        override val fileBaseName: String,
        override val extension: LogOutputExtension = LogOutputExtension.TXT,
    ) : LogOutputConfig {
        override val filter = AppLogType.Filter.Geo
    }

    @SerialName("station")
    data class Station(
        override val timestamp: Long,
        override val fileBaseName: String,
    ) : LogOutputConfig {
        override val filter = AppLogType.Filter.Station
        override val extension = LogOutputExtension.TXT
    }
}
