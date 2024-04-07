package jp.seo.station.ekisagasu.model

sealed interface DataUpdateProgress {
    data class Download(val percent: Int) : DataUpdateProgress

    object Save : DataUpdateProgress
}
