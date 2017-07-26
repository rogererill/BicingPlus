package com.erill.bicingplus.main

import android.util.Log
import com.erill.bicingplus.manager.BicingManager
import com.erill.bicingplus.ws.responses.BicingResponse
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.concurrent.TimeoutException

class MainPresenter(val view: MainView, val bicingManager: BicingManager) {

    var currentResponse: BicingResponse? = null
    var suggestions: ArrayList<String> = ArrayList()

    fun loadStations(infoType: InfoType) : Unit {
        view.showProgress()
        bicingManager.loadStations()
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe(
                        {
                            response ->
                                currentResponse = response
                                createSuggestions(response)
                                view.printStations(response, infoType)
                                view.showSuccess()
                                view.hideProgress()
                        },
                        {
                            error ->
                            if (error.message != null) Log.e("ErrorMain", error.message)
                            if (error is TimeoutException) view.showTimeOutError()
                            else view.showError()
                            view.hideProgress()
                        }
                )
    }

    private fun createSuggestions(response: BicingResponse?) {
        suggestions.clear()
        response?.stations?.forEach {
            val name = it.id + " - " + it.street + ", " + it.number
            suggestions.add(name)
        }
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