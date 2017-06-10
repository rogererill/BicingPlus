package com.erill.bicingplus.main.di

import com.erill.bicingplus.main.MainActivity
import dagger.Subcomponent

@Subcomponent(modules = arrayOf(MainModule::class))
interface MainComponent {
    fun inject(activity: MainActivity)
}