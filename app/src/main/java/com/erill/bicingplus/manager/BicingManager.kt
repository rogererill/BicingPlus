package com.erill.bicingplus.manager

import com.erill.bicingplus.App
import com.erill.bicingplus.ws.BicingApi
import com.erill.bicingplus.ws.responses.BicingResponse
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * Created by Roger on 11/6/17.
 */

class BicingManager(app: App, val bicingApi: BicingApi) {

    init {
        app.component.inject(this)
    }

    fun loadStations() : Observable<BicingResponse>? {
        return bicingApi.getBicingInfo()
    }
}