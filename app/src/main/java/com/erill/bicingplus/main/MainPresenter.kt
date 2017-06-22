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
                        {
                            response ->
                                view.printStations(response)
                                view.hideProgress()
                        },
                        { error -> Log.e("Error", error.message) }
                )
    }
}