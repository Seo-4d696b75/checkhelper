package jp.seo.station.ekisagasu.model

import com.google.android.gms.common.api.ResolvableApiException

sealed interface AppMessage {
    data class Log(val message: String) :AppMessage
    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : AppMessage
    data class ResolvableException(
        val exception: ResolvableApiException,
    ) :AppMessage
    data class Location(
        val lat: Double,
        val lng: Double,
    ): AppMessage
    data class Station(
        val station: jp.seo.station.ekisagasu.Station
    ): AppMessage
}