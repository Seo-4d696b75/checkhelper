package jp.seo.station.ekisagasu.ui.log

enum class LogOutputExtension {
    txt,
    gpx,
}

sealed interface LogOutputConfig : java.io.Serializable {
    val filter: LogFilter
    val extension: LogOutputExtension

    object All : LogOutputConfig {
        override val filter = LogFilter.all
        override val extension = LogOutputExtension.txt
    }

    object System : LogOutputConfig {
        override val filter = LogFilter.system
        override val extension = LogOutputExtension.txt
    }

    data class Geo(
        override val extension: LogOutputExtension,
    ) : LogOutputConfig {
        override val filter = LogFilter.geo
    }

    object Station : LogOutputConfig {
        override val filter = LogFilter.station
        override val extension = LogOutputExtension.txt
    }
}
