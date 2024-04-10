package jp.seo.station.ekisagasu.ui.log

import com.seo4d696b75.android.ekisagasu.data.log.AppLogType

enum class LogOutputExtension {
    txt,
    gpx,
}

sealed interface LogOutputConfig : java.io.Serializable {
    val filter: AppLogType.Filter
    val extension: LogOutputExtension

    data object All : LogOutputConfig {
        override val filter = AppLogType.Filter.All
        override val extension = LogOutputExtension.txt
    }

    data object System : LogOutputConfig {
        override val filter = AppLogType.Filter.System
        override val extension = LogOutputExtension.txt
    }

    data class Geo(override val extension: LogOutputExtension,) : LogOutputConfig {
        override val filter = AppLogType.Filter.Geo
    }

    data object Station : LogOutputConfig {
        override val filter = AppLogType.Filter.Station
        override val extension = LogOutputExtension.txt
    }
}
