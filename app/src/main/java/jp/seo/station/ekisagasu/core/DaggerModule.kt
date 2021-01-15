package jp.seo.station.ekisagasu.core

import androidx.lifecycle.ViewModelStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.seo.station.ekisagasu.utils.ServiceGetter
import javax.inject.Singleton

/**
 * シングルトンスコープで設定したモジュール
 *
 * Application全体を通して共有が必要な依存を注入する
 * @author Seo-4d696b75
 * @version 2021/01/15.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideServiceGetter(): ServiceGetter {
        return ServiceGetter()
    }

    @Singleton
    @Provides
    fun provideViewModelStore(): ViewModelStore {
        return ViewModelStore()
    }
}

