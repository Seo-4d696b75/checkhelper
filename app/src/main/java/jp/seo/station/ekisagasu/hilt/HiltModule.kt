package jp.seo.station.ekisagasu.hilt

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.seo.station.ekisagasu.core.UserDatabase
import jp.seo.station.ekisagasu.core.UserRepository
import jp.seo.station.ekisagasu.repository.*
import jp.seo.station.ekisagasu.repository.impl.AppLoggerImpl
import jp.seo.station.ekisagasu.repository.impl.AppStateRepositoryImpl
import jp.seo.station.ekisagasu.repository.impl.GPSClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @Binds
    @Singleton
    fun bindsLogEmitter(impl: LogEmitterImpl): LogEmitter

}

@Module
@InstallIn(SingletonComponent::class)
interface AppStateModule {
    @Binds
    @Singleton
    fun bindAppStateRepository(impl: AppStateRepositoryImpl): AppStateRepository
}

@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
object UserRepositoryModule {

    @Singleton
    @Provides
    fun provideUserRepository(
        db: UserDatabase
    ): UserRepository {
        return UserRepository(db.userDao, Dispatchers.Main)
    }
}