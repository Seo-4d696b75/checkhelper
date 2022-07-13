package jp.seo.station.ekisagasu.model

import jp.seo.station.ekisagasu.model.Station

data class StationRegister(
    val code: Int,
    val station: Station,
    val numbering: String
)
