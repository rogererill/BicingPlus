package com.erill.bicingplus.main.di

import com.erill.bicingplus.main.MainActivity
import com.erill.bicingplus.main.MainPresenter
import dagger.Module
import dagger.Provides

@Module
class MainModule(val activity: MainActivity) {
    @Provides fun provideMainPresenter() = MainPresenter(activity)
}