package com.seo4d696b75.android.ekisagasu.data.log

import com.google.android.gms.common.api.ResolvableApiException
import com.seo4d696b75.android.ekisagasu.data.database.DataVersion
import com.seo4d696b75.android.ekisagasu.data.station.LatestDataVersion

/**
 * アプリ内部に永続化するユーザー向けのログメッセージ
 *
 * 開発者向けのログは`Timber`の方を使う
 */
sealed interface LogMessage {

    /**
     * エラーとして表示するメッセージ
     */
    sealed interface Error : LogMessage {
        val error: Throwable
    }

    /**
     * [AppLogType.System]
     */
    sealed interface System : LogMessage

    // 駅データに関するメッセージ
    sealed interface Data : System {
        data class Found(val version: DataVersion) : Data
        data class DownloadRequired(val version: LatestDataVersion) : Data
        data class LatestVersionFound(val version: LatestDataVersion) : Data
        data class CheckLatestVersionFailure(override val error: Throwable) :
            Data,
            Error
        data object UpdateSuccess : Data
        data class UpdateFailure(override val error: Throwable) :
            Data,
            Error
    }

    // GPSによる位置情報取得に関するメッセージ
    sealed interface GPS : System {
        data class ResolvableException(val e: ResolvableApiException) : GPS
        data class Start(val interval: Int) : GPS
        data class StartFailure(override val error: Throwable) :
            GPS,
            Error
        data object Stop : GPS
        data class IntervalChanged(val before: Int, val after: Int) : GPS
        data object NoPermission : GPS
    }

    /**
     * [AppLogType.Location]
     */
    data class Location(val lat: Double, val lng: Double,) : LogMessage

    /**
     * [AppLogType.Station]
     */
    data class Station(val station: com.seo4d696b75.android.ekisagasu.data.station.Station,) : LogMessage

    val isError: Boolean
        get() = this is Error

    val type: AppLogType
        get() = when (this) {
            is System -> AppLogType.System
            is Location -> AppLogType.Location
            is Station -> AppLogType.Station
        }
}
