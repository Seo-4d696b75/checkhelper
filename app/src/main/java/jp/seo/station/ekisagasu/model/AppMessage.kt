package jp.seo.station.ekisagasu.model

import android.content.Intent
import com.google.android.gms.common.api.ResolvableApiException
import jp.seo.station.ekisagasu.api.DataLatestInfo
import jp.seo.station.ekisagasu.ui.dialog.DataUpdateType

sealed interface AppMessage {
    data class Log(val message: String) : AppMessage
    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : AppMessage

    data class ResolvableException(
        val message: String,
        val exception: ResolvableApiException,
    ) : AppMessage

    data class Location(
        val lat: Double,
        val lng: Double,
    ) : AppMessage

    data class Station(
        val station: jp.seo.station.ekisagasu.model.Station
    ) : AppMessage

    object FinishApp : AppMessage
    object StartTimer : AppMessage
    data class StartActivityForResult(
        val code: Int,
        val intent: Intent,
    ) : AppMessage

    data class ReceiveActivityResult(
        val code: Int,
        val result: Int,
        val data: Intent?,
    ) : AppMessage

    data class RequestDataUpdate(
        val type: DataUpdateType,
        val info: DataLatestInfo,
    ) : AppMessage
}