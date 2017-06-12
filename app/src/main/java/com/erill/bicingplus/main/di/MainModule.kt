package com.erill.bicingplus.main.di

import com.erill.bicingplus.main.MainActivity
import com.erill.bicingplus.main.MainPresenter
import com.erill.bicingplus.manager.BicingManager
import dagger.Module
import dagger.Provides

@Module
class MainModule(val activity: MainActivity) {
    @Provides fun provideMainPresenter(bicingManager: BicingManager) = MainPresenter(activity, bicingManager)
}