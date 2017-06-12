package com.erill.bicingplus.ws

import com.erill.bicingplus.ws.responses.BicingResponse
import retrofit2.http.GET
import rx.Observable

/**
 * Created by Roger on 11/6/17.
 */

interface BicingApi {
    @GET("stations")
    fun getBicingInfo(): Observable<BicingResponse>
}