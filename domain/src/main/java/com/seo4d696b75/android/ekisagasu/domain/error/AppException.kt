package com.seo4d696b75.android.ekisagasu.domain.error

sealed class AppException(
    message: String?,
    cause: Throwable?,
) : RuntimeException(message, cause)

/**
 * @param cause com.google.android.gms.common.api.ResolvableApiException
 */
class GMSResolvableException(
    cause: Throwable
) : AppException("GMS resolvable api exception", cause)
