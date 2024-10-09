package com.seo4d696b75.android.ekisagasu.data.log

import com.seo4d696b75.android.ekisagasu.data.BuildConfig
import com.seo4d696b75.android.ekisagasu.domain.lifecycle.AppInitializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import timber.log.Timber

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
object TimberInitializerModule {
    @Provides
    @IntoSet
    fun provideTimberInitializer() = object : AppInitializer {
        override fun onCreate() {
            // ログ出力を制御
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            }
        }
    }
}
