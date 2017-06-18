package com.erill.bicingplus.ws.responses

import com.google.gson.annotations.SerializedName

/**
 * Created by Roger on 17/6/17.
 */
enum class BikeType(name: String = "BIKE") {

    @SerializedName("BIKE")
    REGULAR("BIKE"),

    @SerializedName("BIKE-ELECTRIC")
    ELECTRIC("BIKE-ELECTRIC")


}