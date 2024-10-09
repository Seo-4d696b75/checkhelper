package jp.seo.station.ekisagasu.config

import android.content.Context
import android.os.Build
import com.seo4d696b75.android.ekisagasu.domain.config.AppConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.seo.station.ekisagasu.BuildConfig
import jp.seo.station.ekisagasu.R

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {

    // appモジュールのBuildConfigを他モジュールから参照するだけの依存
    @Provides
    fun provideAppConfig(
        @ApplicationContext context: Context,
    ): AppConfig = object : AppConfig {
        override val versionName: String
            get() = BuildConfig.VERSION_NAME
        override val deviceName: String
            get() = Build.DEVICE
        override val appName: String
            get() = context.getString(R.string.app_name)
    }
}
