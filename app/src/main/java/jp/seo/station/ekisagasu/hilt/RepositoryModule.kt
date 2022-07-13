package jp.seo.station.ekisagasu.hilt

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.seo.station.ekisagasu.repository.*
import jp.seo.station.ekisagasu.repository.impl.*
import jp.seo.station.ekisagasu.search.KdTree
import jp.seo.station.ekisagasu.search.NearestSearch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Main
}

@Module
@InstallIn(SingletonComponent::class)
interface LocationModule {

    @Binds
    @Singleton
    fun bindLocationRepository(impl: GPSClient): LocationRepository
}

@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
interface LoggerModule {

    @Binds
    @Singleton
    fun bindsLogger(impl: LogRepositoryImpl): LogRepository

    @Binds
    @Singleton
    fun bindsLogEmitter(impl: AppLoggerImpl): AppLogger

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
interface LogModule {

    @Binds
    @Singleton
    fun bindLogRepository(impl: LogRepositoryImpl): LogRepository
}

@Module
@InstallIn(SingletonComponent::class)
interface SettingModule {

    @Singleton
    @Binds
    fun bindSettingRepository(impl: UserSettingRepositoryImpl): UserSettingRepository

}

@Module
@InstallIn(SingletonComponent::class)
interface SearchModule {

    @Singleton
    @Binds
    fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Singleton
    @Binds
    fun bindNearestSearch(impl: KdTree): NearestSearch
}

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Singleton
    @Binds
    fun bindDataRepository(impl: DataRepositoryImpl): DataRepository
}

@Module
@InstallIn(SingletonComponent::class)
interface NavigationModule {

    @Singleton
    @Binds
    fun bindNavigation(impl: NavigationRepositoryImpl): NavigationRepository
}

@Module
@InstallIn(SingletonComponent::class)
interface PrefectureModule {

    @Singleton
    @Binds
    fun bindPrefectureRepository(impl: PrefectureRepositoryImpl): PrefectureRepository
}
