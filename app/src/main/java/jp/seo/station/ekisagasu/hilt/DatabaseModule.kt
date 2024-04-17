package jp.seo.station.ekisagasu.hilt

import android.content.Context
import androidx.room.Room
import com.seo4d696b75.android.ekisagasu.data.database.StationDao
import com.seo4d696b75.android.ekisagasu.data.database.StationDatabase
import com.seo4d696b75.android.ekisagasu.data.database.UserDao
import com.seo4d696b75.android.ekisagasu.data.database.UserDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun provideStationDatabase(
        @ApplicationContext context: Context,
    ): StationDao {
        return Room.databaseBuilder(context, StationDatabase::class.java, "station_db")
            .fallbackToDestructiveMigration()
            .build()
            .dao
    }

    @Singleton
    @Provides
    fun provideUserDatabase(
        @ApplicationContext context: Context,
    ): UserDao {
        return Room.databaseBuilder(context, UserDatabase::class.java, "user_db")
            .fallbackToDestructiveMigration()
            .build()
            .userDao
    }
}
