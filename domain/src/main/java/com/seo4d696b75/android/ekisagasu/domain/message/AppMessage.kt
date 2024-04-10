package com.seo4d696b75.android.ekisagasu.domain.message

import com.seo4d696b75.android.ekisagasu.domain.dataset.update.DataUpdateType
import com.seo4d696b75.android.ekisagasu.domain.dataset.LatestDataVersion

sealed interface AppMessage {

    //　TODO .aar の com.google.android.gms.common.api.ResolvableApiException をimportできない
    data class ResolvableException(val exception: Exception) : AppMessage

    data object FinishApp : AppMessage

    data object StartTimer : AppMessage

    sealed interface Data : AppMessage {
        data class CheckLatestVersionFailure(val error: Throwable) : Data

        data object VersionUpToDate : Data
        data class ConfirmUpdate(val type: DataUpdateType, val info: LatestDataVersion) : Data

        data class CancelUpdate(val type: DataUpdateType) : Data

        data class RequestUpdate(val type: DataUpdateType, val info: LatestDataVersion) : Data

        data object UpdateSuccess : Data

        data class UpdateFailure(val type: DataUpdateType, val error: Throwable) : Data
    }
}
