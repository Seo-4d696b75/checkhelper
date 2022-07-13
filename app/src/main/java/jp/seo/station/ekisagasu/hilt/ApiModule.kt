package jp.seo.station.ekisagasu.hilt

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.core.APIClient
import jp.seo.station.ekisagasu.core.getAPIClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Singleton
    @Provides
    fun provideAPIClient(
        @ApplicationContext context: Context
    ): APIClient {
        val baseURL = context.getString(R.string.api_url_base)
        return getAPIClient(baseURL)
    }
}
