package com.erill.bicingplus.di

import com.erill.bicingplus.App
import com.erill.bicingplus.main.di.MainComponent
import com.erill.bicingplus.main.di.MainModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(AppModule::class))
interface AppComponent {
    fun inject(app: App)
    fun plus(homeModule: MainModule): MainComponent
}