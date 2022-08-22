package jp.seo.station.ekisagasu.repository

import com.google.android.gms.common.api.ResolvableApiException
import kotlinx.coroutines.CoroutineScope

interface AppLogger {
    fun CoroutineScope.log(message: String)
    fun CoroutineScope.error(message: String, cause: Throwable? = null)
    fun CoroutineScope.requestExceptionResolved(message: String, e: ResolvableApiException)
}
