package jp.seo.station.ekisagasu.hilt

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.seo.station.ekisagasu.repository.AppLogger
import jp.seo.station.ekisagasu.repository.AppStateRepository
import jp.seo.station.ekisagasu.repository.DataRepository
import jp.seo.station.ekisagasu.repository.LocationRepository
import jp.seo.station.ekisagasu.repository.LogRepository
import jp.seo.station.ekisagasu.repository.NavigationRepository
import jp.seo.station.ekisagasu.repository.PrefectureRepository
import jp.seo.station.ekisagasu.repository.SearchRepository
import jp.seo.station.ekisagasu.repository.UserSettingRepository
import jp.seo.station.ekisagasu.repository.impl.AppLoggerImpl
import jp.seo.station.ekisagasu.repository.impl.AppStateRepositoryImpl
import jp.seo.station.ekisagasu.repository.impl.DataRepositoryImpl
import jp.seo.station.ekisagasu.repository.impl.GPSClient
import jp.seo.station.ekisagasu.repository.impl.LogRepositoryImpl
import jp.seo.station.ekisagasu.repository.impl.NavigationRepositoryImpl
import jp.seo.station.ekisagasu.repository.impl.PrefectureRepositoryImpl
import jp.seo.station.ekisagasu.repository.impl.SearchRepositoryImpl
import jp.seo.station.ekisagasu.repository.impl.UserSettingRepositoryImpl
import jp.seo.station.ekisagasu.search.KdTree
import jp.seo.station.ekisagasu.search.NearestSearch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ExperimentalSerializationApi
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
interface LogModule {

    @Binds
    @Singleton
    fun bindsLogRepository(impl: LogRepositoryImpl): LogRepository

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

@ExperimentalSerializationApi
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
