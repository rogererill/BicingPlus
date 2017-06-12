package com.erill.bicingplus.ws.responses

import com.google.gson.annotations.SerializedName

/**
 * Created by Roger on 11/6/17.
 */

data class BicingResponse (
        @SerializedName("stations") val stations: List<Station>,
        @SerializedName("updateTime") val updateTime: Long
)