package jp.seo.station.ekisagasu.core

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import jp.seo.station.ekisagasu.utils.ServiceGetter

/**
 * Activityスコープで設定したモジュール
 *
 * Activity全体を通して共有が必要な依存を注入する
 * @author Seo-4d696b75
 * @version 2021/01/15.
 */
@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {
    @ActivityScoped
    @Provides
    fun provideServiceGetter(): ServiceGetter {
        return ServiceGetter()
    }

}

