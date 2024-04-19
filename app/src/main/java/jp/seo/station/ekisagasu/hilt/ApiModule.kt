package jp.seo.station.ekisagasu.hilt

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.seo4d696b75.android.ekisagasu.data.api.StationDataService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HttpClientModule {
    @Singleton
    @Provides
    fun provideHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }
}

@Module
@ExperimentalSerializationApi
@InstallIn(SingletonComponent::class)
object RetrofitModule {
    @Singleton
    @Provides
    fun provideRetrofit(
        client: OkHttpClient,
        json: Json,
    ): Retrofit {
        // FIXME use BuildConfig
        val baseURL = "https://cdn.jsdelivr.net/gh/Seo-4d696b75/"
        val contentType = MediaType.get("application/json")
        val converter = json.asConverterFactory(contentType)
        return Retrofit.Builder()
            .baseUrl(baseURL)
            .client(client)
            .addConverterFactory(converter)
            .build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Singleton
    @Provides
    fun provideAPIClient(retrofit: Retrofit): StationDataService = retrofit.create(StationDataService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object JsonModule {
    @Provides
    fun provideJson() =
        Json {
            ignoreUnknownKeys = true
        }
}
