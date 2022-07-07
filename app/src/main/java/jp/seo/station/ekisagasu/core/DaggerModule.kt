package jp.seo.station.ekisagasu.core

import android.content.Context
import android.os.Handler
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.search.KdTree
import javax.inject.Singleton

/**
 * Activityスコープで設定したモジュール
 *
 * Activity全体を通して共有が必要な依存を注入する
 * @author Seo-4d696b75
 * @version 2021/01/15.
 */
@Module
@InstallIn(ActivityComponent::class)
object ActivityModule

/**
 * Applicationスコープで設定するモジュール
 *
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideViewModelStore(): ViewModelStore {
        return ViewModelStore()
    }

    @Singleton
    @Provides
    fun provideAPIClient(
        @ApplicationContext context: Context
    ): APIClient {
        val baseURL = context.getString(R.string.api_url_base)
        return getAPIClient(baseURL)
    }

    @Singleton
    @Provides
    fun provideStationDatabase(
        @ApplicationContext context: Context
    ): StationDatabase {
        return Room.databaseBuilder(context, StationDatabase::class.java, "station_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Singleton
    @Provides
    fun provideUserDatabase(
        @ApplicationContext context: Context
    ): UserDatabase {
        return Room.databaseBuilder(context, UserDatabase::class.java, "user_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Singleton
    @Provides
    fun providePrefectureRepository(): PrefectureRepository {
        return PrefectureRepository()
    }

    @Singleton
    @Provides
    fun provideKdTree(db: StationDatabase): KdTree {
        return KdTree(db.dao)
    }

    @Singleton
    @Provides
    fun provideStationRepository(
        db: StationDatabase,
        api: APIClient,
        tree: KdTree,
    ): StationRepository {
        return StationRepository(db.dao, api, tree)
    }

    @Singleton
    @Provides
    fun provideNavigator(
        tree: KdTree,
        db: StationDatabase
    ): NavigationRepository {
        return NavigationRepository(tree, db.dao)
    }
}

