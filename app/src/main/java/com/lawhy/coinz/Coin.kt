package com.lawhy.coinz

import android.location.Location
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.annotations.Marker

class Coin( val id: String,val  currency: String,val value: Double, mapMarker: Marker) {

    val marker: Marker? = mapMarker // The map marker is allowed to be null if the coin has been collected

    // Coins can be collected within 25 meters
    fun distToLocation(location: Location): Double{
        val curLatLng = LatLng(location.latitude, location.longitude)
        return curLatLng.distanceTo(this.marker?.position)
    }


}