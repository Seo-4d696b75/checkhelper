package jp.seo.station.ekisagasu.hilt

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.seo.station.ekisagasu.R
import jp.seo.station.ekisagasu.api.APIClient
import jp.seo.station.ekisagasu.api.getAPIClient
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@ExperimentalSerializationApi
@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Singleton
    @Provides
    fun provideAPIClient(
        @ApplicationContext context: Context,
        json: Json,
    ): APIClient {
        val baseURL = context.getString(R.string.api_url_base)
        return getAPIClient(baseURL, json)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object JsonModule {
    @Provides
    fun provideJson() = Json {
        ignoreUnknownKeys = true
    }
}
