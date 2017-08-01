package com.erill.bicingplus.main

import android.util.Log
import com.erill.bicingplus.DEFAULT_LAT
import com.erill.bicingplus.DEFAULT_LON
import com.erill.bicingplus.manager.BicingManager
import com.erill.bicingplus.ws.responses.BicingResponse
import com.erill.bicingplus.ws.responses.Station
import com.google.android.gms.maps.model.LatLng
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
            val name = printStationForSuggestion(it)
            suggestions.add(name)
        }
    }

    private fun printStationForSuggestion(it: Station) = it.id + " - " + it.street + ", " + it.number

    fun onChangeSetting(currentInfoType: InfoType) {
        val newInfoType: InfoType
        when (currentInfoType) {
            InfoType.BIKES -> newInfoType = InfoType.PARKING
            InfoType.PARKING -> newInfoType = InfoType.BIKES
        }
        view.setInfoType(newInfoType)
        view.printStations(currentResponse, newInfoType)
    }

    private fun getStationFromSuggestion(suggestionName: String): Station? {
        val id = suggestionName.split("-").getOrNull(0)?.trim() ?: return null
        return currentResponse?.stations?.find {
            it.id == id
        }
    }

    fun getLatLongForPosition(suggestionName: String): LatLng? {
        val station = getStationFromSuggestion(suggestionName)
        val lat = station?.lat?.toDouble() ?: DEFAULT_LAT
        val lon = station?.lon?.toDouble() ?: DEFAULT_LON
        return LatLng(lat, lon)
    }

    fun getSuggestion(suggestionName: String): CharSequence {
        val station = getStationFromSuggestion(suggestionName)
        if (station != null) return printStationForSuggestion(station)
        return ""
    }
}