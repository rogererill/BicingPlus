package com.erill.bicingplus.di

import android.util.Log
import com.erill.bicingplus.App
import com.erill.bicingplus.BASE_URL
import com.erill.bicingplus.BuildConfig
import com.erill.bicingplus.manager.BicingManager
import com.erill.bicingplus.ws.BicingApi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import rx.schedulers.Schedulers
import javax.inject.Singleton

@Module class AppModule(val app: App) {
    @Provides @Singleton fun provideApp() = app
    @Provides @Singleton fun provideBicingManager(bicingApi: BicingApi) : BicingManager = BicingManager(app, bicingApi)

    @Provides
    fun provideLogger(): HttpLoggingInterceptor {
        val httpLoggingInterceptor = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { message -> Log.d("OkHTTP", message) })
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return httpLoggingInterceptor
    }

    @Provides
    @Singleton
    fun provideClient(logger: HttpLoggingInterceptor): OkHttpClient {
        val okHttpClientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            logger.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(logger)
        }
        return okHttpClientBuilder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(httpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
                .client(httpClient)
                .build()
    }

    @Provides
    @Singleton
    fun provideService(retrofit: Retrofit): BicingApi {
        return retrofit.create(BicingApi::class.java)
    }
}