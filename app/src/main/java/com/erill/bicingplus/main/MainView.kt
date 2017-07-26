package com.erill.bicingplus.main

import com.erill.bicingplus.ws.responses.BicingResponse

/**
 * Created by Roger on 10/6/17.
 */
interface MainView {
    fun showProgress()
    fun hideProgress()
    fun printStations(response: BicingResponse?, infoType: InfoType)
    fun showSuccess()
    fun showError()
    fun setInfoType(infoType: InfoType)
    fun showTimeOutError()
}