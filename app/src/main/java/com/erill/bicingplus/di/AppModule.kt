package com.erill.bicingplus.di

import com.erill.bicingplus.App
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module class AppModule(val app: App) {
    @Provides @Singleton fun provideApp() = app
}