package jp.seo.station.ekisagasu.model

import com.google.android.gms.common.api.ResolvableApiException

sealed interface AppMessage {
    data class AppLog(val message: String) : AppMessage
    data class AppError(
        val message: String,
        val displayedMessage: String,
        val cause: Throwable? = null,
    ) : AppMessage

    data class AppResolvableException(val exception: ResolvableApiException) : AppMessage
}