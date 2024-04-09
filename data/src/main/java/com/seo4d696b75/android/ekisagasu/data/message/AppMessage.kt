package com.seo4d696b75.android.ekisagasu.data.message

import com.google.android.gms.common.api.ResolvableApiException
import com.seo4d696b75.android.ekisagasu.data.station.DataUpdateType
import com.seo4d696b75.android.ekisagasu.data.station.LatestDataVersion

sealed interface AppMessage {

    data class ResolvableException(
        val exception: ResolvableApiException,
    ) : AppMessage

    data object FinishApp : AppMessage

    data object StartTimer : AppMessage

    sealed interface Data : AppMessage {
        data class CheckLatestVersionFailure(
            val error: Throwable,
        ) : Data

        data object VersionUpToDate : Data
        data class ConfirmUpdate(
            val type: DataUpdateType,
            val info: LatestDataVersion,
        ) : Data

        data class CancelUpdate(
            val type: DataUpdateType,
        ) : Data

        data class RequestUpdate(
            val type: DataUpdateType,
            val info: LatestDataVersion,
        ) : Data

        data object UpdateSuccess : Data

        data class UpdateFailure(
            val type: DataUpdateType,
            val error: Throwable,
        ) : Data
    }
}
