package jp.seo.station.ekisagasu.hilt

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.seo.station.ekisagasu.database.StationDao
import jp.seo.station.ekisagasu.database.StationDatabase
import jp.seo.station.ekisagasu.database.UserDao
import jp.seo.station.ekisagasu.database.UserDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideStationDatabase(
        @ApplicationContext context: Context
    ): StationDao {
        return Room.databaseBuilder(context, StationDatabase::class.java, "station_db")
            .fallbackToDestructiveMigration()
            .build()
            .dao
    }

    @Singleton
    @Provides
    fun provideUserDatabase(
        @ApplicationContext context: Context
    ): UserDao {
        return Room.databaseBuilder(context, UserDatabase::class.java, "user_db")
            .fallbackToDestructiveMigration()
            .build()
            .userDao
    }
}
