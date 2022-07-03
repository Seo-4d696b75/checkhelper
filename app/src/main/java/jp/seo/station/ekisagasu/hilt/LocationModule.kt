package jp.seo.station.ekisagasu.hilt

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.seo.station.ekisagasu.repository.AppLogger
import jp.seo.station.ekisagasu.repository.LocationRepository
import jp.seo.station.ekisagasu.repository.LogEmitter
import jp.seo.station.ekisagasu.repository.impl.AppLoggerImpl
import jp.seo.station.ekisagasu.repository.impl.GPSClient
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideLocationRepository(
        @ApplicationContext context: Context,
        logger: LogEmitter,
    ): LocationRepository {
        return GPSClient(context, logger, Dispatchers.Main)
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface LoggerModule {

    @Binds
    @Singleton
    fun bindsLogger(impl: AppLoggerImpl): AppLogger

}