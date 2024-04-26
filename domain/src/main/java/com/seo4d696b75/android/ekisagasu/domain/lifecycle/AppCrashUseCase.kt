package com.seo4d696b75.android.ekisagasu.domain.lifecycle

import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
import com.seo4d696b75.android.ekisagasu.domain.log.LogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class AppCrashUseCase @Inject constructor(
    private val logRepository: LogRepository,
) {
    operator fun invoke(e: Throwable) = runBlocking(Dispatchers.Default) {
        // ログ記録
        logRepository.write(
            type = AppLogType.System,
            message = "UnhandledException:\n$e",
            isError = true,
        )
        logRepository.onAppFinish()
    }
}
