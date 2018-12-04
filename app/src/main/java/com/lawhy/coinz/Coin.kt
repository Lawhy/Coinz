package com.lawhy.coinz

import android.location.Location
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.annotations.Marker
import java.io.Serializable

class Coin(val id: String, val currency: String, val value: Double, mapMarker: Marker?) : Serializable {

    /** A very important class that preserves coins' information,
     *    1. id
     *    2. currency type
     *    3. value
     *    4. a map marker that can be used to specify the position of coin
     *  Coins can be stored in a Wallet instance
     * */

    val marker: Marker? = mapMarker // The map marker is allowed to be null if the coin has been collected

    // Coins can be collected within 25 meters
    // The only use of marker here is to provide location before collecting
    fun distToLocation(location: Location): Double {
        val curLatLng = LatLng(location.latitude, location.longitude)
        return curLatLng.distanceTo(this.marker?.position)
    }

    override fun toString(): String {
        return "A ${this.currency} of value %.3f".format(this.value)
    }

}