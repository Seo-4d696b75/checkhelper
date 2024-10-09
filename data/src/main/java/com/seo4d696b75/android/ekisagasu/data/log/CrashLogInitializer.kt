package com.seo4d696b75.android.ekisagasu.data.log

import com.seo4d696b75.android.ekisagasu.domain.lifecycle.AppCrashUseCase
import com.seo4d696b75.android.ekisagasu.domain.lifecycle.AppInitializer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

class CrashLogInitializer @Inject constructor(
    private val crashUseCase: AppCrashUseCase,
) : AppInitializer {
    override fun onCreate() {
        // 未補足の例外を処理
        var crashStarting = false
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            if (crashStarting) return@setDefaultUncaughtExceptionHandler
            crashStarting = true
            try {
                Timber.e(e, "caught in DefaultUncaughtExceptionHandler")
                crashUseCase(e)
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
