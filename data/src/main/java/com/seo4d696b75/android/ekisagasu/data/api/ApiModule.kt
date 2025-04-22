package com.seo4d696b75.android.ekisagasu.data.api

import com.seo4d696b75.android.ekisagasu.data.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HttpClientModule {
    @Singleton
    @Provides
    fun provideHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) {
                val interceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                addInterceptor(interceptor)
            }
        }.build()
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
        val contentType = "application/json".toMediaType()
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
    fun provideJson() = Json { ignoreUnknownKeys = true }
}
