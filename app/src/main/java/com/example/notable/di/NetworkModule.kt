package com.example.notable.di

import com.example.notable.core.Constants
import com.example.notable.data.NotableApi
import com.example.notable.data.TokenManager
import com.example.notable.data.TokenRefreshInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.inject.Provider

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideTokenRefreshInterceptor(
        tokenManager: TokenManager,
        apiProvider: Provider<NotableApi> // ✅ Use Provider
    ): TokenRefreshInterceptor {
        return TokenRefreshInterceptor(tokenManager, apiProvider)
    }


    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenRefreshInterceptor: TokenRefreshInterceptor, // ✅ Inject the interceptor
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(tokenRefreshInterceptor) // ✅ Add it to the chain
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideNotableApi(retrofit: Retrofit): NotableApi {
        return retrofit.create(NotableApi::class.java)
    }
}
