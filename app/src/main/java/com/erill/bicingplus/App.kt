package com.erill.bicingplus

import android.app.Application
import com.erill.bicingplus.di.AppComponent
import com.erill.bicingplus.di.AppModule
import com.erill.bicingplus.di.DaggerAppComponent

class App : Application() {

    val component: AppComponent by lazy {
        DaggerAppComponent
                .builder()
                .appModule(AppModule(this))
                .build()
    }

    override fun onCreate() {
        super.onCreate()
        component.inject(this)
    }
}