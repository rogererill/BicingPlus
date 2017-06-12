package com.erill.bicingplus.ws.responses

import com.google.gson.annotations.SerializedName

/**
 * Created by Roger on 11/6/17.
 */

data class Station(
        @SerializedName("id") val id: String,
        @SerializedName("type") val type: String,
        @SerializedName("latitude") val lat: String,
        @SerializedName("longitude") val lon: String,
        @SerializedName("streetName") val street: String,
        @SerializedName("streetNumber") val number: String,
        @SerializedName("altitude") val altitude: String,
        @SerializedName("slots") val slots: String,
        @SerializedName("bikes") val bikes: String,
        @SerializedName("nearbyStations") val nearbyStations: String,
        @SerializedName("status") val status: String
)