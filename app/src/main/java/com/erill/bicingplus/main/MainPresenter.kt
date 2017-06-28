package com.erill.bicingplus.main

import android.util.Log
import com.erill.bicingplus.manager.BicingManager
import com.erill.bicingplus.ws.responses.BicingResponse
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

class MainPresenter(val view: MainView, val bicingManager: BicingManager) {

    var currentResponse: BicingResponse? = null

    fun loadStations(infoType: InfoType) : Unit {
        view.showProgress()
        bicingManager.loadStations()
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe(
                        {
                            response ->
                                currentResponse = response
                                view.printStations(response, infoType)
                                view.showSuccess()
                                view.hideProgress()
                        },
                        {
                            error -> Log.e("ErrorMain", error.message)
                            view.showError()
                            view.hideProgress()
                        }
                )
    }

    fun onChangeSetting(currentInfoType: InfoType) {
        val newInfoType: InfoType
        when (currentInfoType) {
            InfoType.BIKES -> newInfoType = InfoType.PARKING
            InfoType.PARKING -> newInfoType = InfoType.BIKES
        }
        view.setInfoType(newInfoType)
        view.printStations(currentResponse, newInfoType)
    }
}