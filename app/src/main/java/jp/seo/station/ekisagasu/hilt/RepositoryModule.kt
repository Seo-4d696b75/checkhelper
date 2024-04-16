package jp.seo.station.ekisagasu.hilt

import com.seo4d696b75.android.ekisagasu.data.kdtree.KdTree
import com.seo4d696b75.android.ekisagasu.data.location.LocationRepositoryImpl
import com.seo4d696b75.android.ekisagasu.data.log.LogRepositoryImpl
import com.seo4d696b75.android.ekisagasu.data.message.AppStateRepositoryImpl
import com.seo4d696b75.android.ekisagasu.data.navigator.NavigatorRepositoryImpl
import com.seo4d696b75.android.ekisagasu.data.search.StationSearchRepositoryImpl
import com.seo4d696b75.android.ekisagasu.data.station.DataRepositoryImpl
import com.seo4d696b75.android.ekisagasu.data.station.PrefectureRepositoryImpl
import com.seo4d696b75.android.ekisagasu.data.user.UserSettingRepositoryImpl
import com.seo4d696b75.android.ekisagasu.domain.dataset.DataRepository
import com.seo4d696b75.android.ekisagasu.domain.dataset.PrefectureRepository
import com.seo4d696b75.android.ekisagasu.domain.kdtree.NearestSearch
import com.seo4d696b75.android.ekisagasu.domain.location.LocationRepository
import com.seo4d696b75.android.ekisagasu.domain.log.LogRepository
import com.seo4d696b75.android.ekisagasu.domain.message.AppStateRepository
import com.seo4d696b75.android.ekisagasu.domain.navigator.NavigatorRepository
import com.seo4d696b75.android.ekisagasu.domain.search.StationSearchRepository
import com.seo4d696b75.android.ekisagasu.domain.user.UserSettingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
    fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository
}

@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
interface LogModule {
    @Binds
    @Singleton
    fun bindsLogRepository(impl: LogRepositoryImpl): LogRepository
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
    fun bindSearchRepository(impl: StationSearchRepositoryImpl): StationSearchRepository

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
    fun bindNavigation(impl: NavigatorRepositoryImpl): NavigatorRepository
}

@Module
@InstallIn(SingletonComponent::class)
interface PrefectureModule {
    @Singleton
    @Binds
    fun bindPrefectureRepository(impl: PrefectureRepositoryImpl): PrefectureRepository
}
