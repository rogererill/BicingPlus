package com.erill.bicingplus.main

import android.util.Log
import com.erill.bicingplus.manager.BicingManager
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class MainPresenter(val view: MainView, val bicingManager: BicingManager) {

    fun loadStations() : Unit {
        bicingManager.loadStations()
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe(
                        { response -> Log.d("Next", "Estacions: ${response.stations.size}") },
                        { error -> Log.e("Error", error.message) }
                )
    }
}