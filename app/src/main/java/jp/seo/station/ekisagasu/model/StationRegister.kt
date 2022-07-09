package jp.seo.station.ekisagasu.model

import jp.seo.station.ekisagasu.Station

data class StationRegister(
    val code: Int,
    val station: Station,
    val numbering: String
)
