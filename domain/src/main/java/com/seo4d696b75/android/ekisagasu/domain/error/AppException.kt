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

/**
 * GPS位置情報が使用できない
 */
class UnavailableLocationException(message: String?) : AppException(message, null)

/**
 * 駅データの最新バージョン取得に失敗した
 */
class CheckLatestDataVersionException(cause: Throwable) : AppException("failed to get latest data version", cause)
