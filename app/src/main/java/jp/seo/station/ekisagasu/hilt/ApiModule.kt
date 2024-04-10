package jp.seo.station.ekisagasu.hilt

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.seo4d696b75.android.ekisagasu.data.api.APIClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.seo.station.ekisagasu.R
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
        @ApplicationContext context: Context,
        client: OkHttpClient,
        json: Json,
    ): Retrofit {
        val baseURL = context.getString(R.string.api_url_base)
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
    fun provideAPIClient(retrofit: Retrofit): APIClient = retrofit.create(APIClient::class.java)
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
