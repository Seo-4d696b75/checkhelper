package com.seo4d696b75.android.ekisagasu.data.log

import com.seo4d696b75.android.ekisagasu.domain.lifecycle.AppFinishUseCase
import com.seo4d696b75.android.ekisagasu.domain.lifecycle.AppInitializer
import com.seo4d696b75.android.ekisagasu.domain.log.AppLogType
import com.seo4d696b75.android.ekisagasu.domain.log.LogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

class CrashLogInitializer @Inject constructor(
    private val logRepository: LogRepository,
    private val appFinishUseCase: AppFinishUseCase,
) : AppInitializer {
    override fun onCreate() {
        // 未補足の例外を処理
        var crashStarting = false
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            if (crashStarting) return@setDefaultUncaughtExceptionHandler
            crashStarting = true
            try {
                runBlocking(Dispatchers.Default) {
                    // ログ記録
                    logRepository.write(
                        type = AppLogType.System,
                        message = "UnhandledException:\n${e.formatStackTrace()}",
                        isError = true,
                    )
                    // 終了処理
                    appFinishUseCase()
                }
            } finally {
                Thread.setDefaultUncaughtExceptionHandler(defaultHandler)
                throw e
            }
        }
    }
}

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface CrashLogInitializerModule {
    @Binds
    @IntoSet
    @Singleton
    fun bind(impl: CrashLogInitializer): AppInitializer
}
