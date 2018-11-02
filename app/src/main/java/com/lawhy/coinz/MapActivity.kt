package com.lawhy.coinz

import android.graphics.*
import android.graphics.drawable.Drawable
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode

//Geo-json libraries
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.google.gson.JsonObject
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.style.layers.LineLayer
import org.json.JSONObject
import java.io.InputStream
import java.lang.Exception

class MapActivity : AppCompatActivity(), PermissionsListener, LocationEngineListener, OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private lateinit var permissionManager: PermissionsManager
    private lateinit var originLoaction: Location
    private lateinit var collectButton: Button
    private lateinit var popupMenuButton: FloatingActionButton

    private var locationEngine : LocationEngine? = null
    private var locationLayerPlugin: LocationLayerPlugin? = null

    private val tag = "MapActivity"
    private var mapCoins = ArrayList<Coin>() // Store the coins' (on the map) information
    private var collectedCoins = ArrayList<Coin>() // Store the current collected coins
    private var exchangeRates = HashMap<String, Double>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        Mapbox.getInstance(applicationContext, getString(R.string.access_token))

        mapView = findViewById(R.id.mapboxMapView)
        collectButton = findViewById(R.id.collectButton)
        popupMenuButton = findViewById(R.id.popupMenuButton)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

    }

    fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)){
            Log.d(tag, "Permissions are grated")
            initializeLocationEngine()
            initializeLocationLayer()
        } else {
            permissionManager = PermissionsManager(this)
            permissionManager.requestLocationPermissions(this)
            Log.d(tag, "Permissions are not granted.")
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initializeLocationEngine() {
        locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
        locationEngine?.apply {
            interval = 5000 // preferably every 5 seconds
            fastestInterval = 1000 // at most every second
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }

        val lastlocation = locationEngine?.lastLocation
        if (lastlocation != null) {
            originLoaction = lastlocation
            setCameraPosition(lastlocation)
        } else {
            locationEngine?.addLocationEngineListener(this)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initializeLocationLayer() {

        if (mapView == null) { Log.d(tag, "mapView is null"); return}
        if (map == null) { Log.d(tag, "map is null"); return}

        locationLayerPlugin = LocationLayerPlugin(mapView!!, map!!, locationEngine)
        locationLayerPlugin?.setLocationLayerEnabled(true)
        locationLayerPlugin?.cameraMode = CameraMode.TRACKING
        locationLayerPlugin?.renderMode = RenderMode.COMPASS
    }

    private fun setCameraPosition(location: Location) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude), 13.0))
    }

    fun readMapFromInternal(): String {

        var json: String
        try {
            val stream: InputStream = openFileInput("map_today.geojson")
            json = stream.bufferedReader().use { it.readText() }
        } catch (ex: Exception) {
            ex.printStackTrace()
            return ""
        }
        return json
    }

    fun exchangeRatesToday() {
        val jsonString = readMapFromInternal()
        if (jsonString == "") {
            Log.d(tag, "Coinz map is null!")
        } else {
            // Parse Geo-Json file and obtain the information we want
            val fc: List<Feature>? = FeatureCollection.fromJson(jsonString).features()
            val jsonObject = JSONObject(jsonString)
            val rates = jsonObject.get("rates") as JSONObject

            exchangeRates["SHIL"] = rates.getDouble("SHIL")
            exchangeRates["DOLR"] = rates.getDouble("DOLR")
            exchangeRates["QUID"] = rates.getDouble("QUID")
            exchangeRates["PENY"] = rates.getDouble("PENY")
            Log.d(tag, "Today's exchange rates: $rates")
        }
    }

    fun initializeLocalMapToday( ) {

        // Add Geo-json features on the map
        val jsonString = readMapFromInternal()
        if (jsonString == "") {
            Log.d(tag, "Coinz map is null!")
        } else {
            Log.d(tag, "Enable Coinz Map!")

            val jsonSource = GeoJsonSource("geojson", jsonString)
            map.addSource(jsonSource)
            map.addLayer(LineLayer("geojson", "geojson"))

            // Parse Geo-Json file and obtain the information we want
            val fc: List<Feature>? = FeatureCollection.fromJson(jsonString).features()

            for (f in fc!!.iterator()) {

                // Extract the properties
                val properties:JsonObject? = f.properties()
                if (properties == null) {
                    Log.d(tag, "Empty properties for the current feature.")
                    continue
                }
                val id = properties.get("id").asString
                val value = properties.get("value").asDouble
                val currency = properties.get("currency").asString
                val symbol = properties.get("marker-symbol").asInt
                val color = properties["marker-color"].asString

                // Using the IconUtils class methods to combine color and number on the same icon
                var icMarker = ContextCompat.getDrawable(this, R.drawable.ic_roundmarker)
                var colorFilter = LightingColorFilter(Color.parseColor(color), Color.parseColor(color))
                icMarker?.colorFilter = colorFilter

                var icNumber : Drawable? = null
                when(symbol) {
                    0 -> icNumber = ContextCompat.getDrawable(this, R.drawable.ic_zero)
                    1 -> icNumber = ContextCompat.getDrawable(this, R.drawable.ic_one)
                    2 -> icNumber = ContextCompat.getDrawable(this, R.drawable.ic_two)
                    3 -> icNumber = ContextCompat.getDrawable(this, R.drawable.ic_three)
                    4 -> icNumber = ContextCompat.getDrawable(this, R.drawable.ic_four)
                    5 -> icNumber = ContextCompat.getDrawable(this, R.drawable.ic_five)
                    6 -> icNumber = ContextCompat.getDrawable(this, R.drawable.ic_six)
                    7 -> icNumber = ContextCompat.getDrawable(this, R.drawable.ic_seven)
                    8 -> icNumber = ContextCompat.getDrawable(this, R.drawable.ic_eight)
                    9 -> icNumber = ContextCompat.getDrawable(this, R.drawable.ic_nine)
                    else -> Log.d(tag, "Invalid number on a coin is detected!")
                }

                var combinedDrawable = IconUtils.combineDrawable(icMarker, icNumber)
                var icon = IconUtils.drawableToIcon(this, combinedDrawable)

                // Extract the geometric information and add markers
                val point = f.geometry()
                if (point is Point) {

                    val pos = LatLng(point.latitude(), point.longitude())
                    val marker = map.addMarker(MarkerOptions()
                            .title(currency)
                            .snippet("Value: $value" )
                            .icon(icon)
                            .position(pos))

                    val coin = Coin(id, currency, value, marker) // Parse the information into a coin object
                    mapCoins.add(coin)
                }
            }

            Log.d(tag, "Map is fully prepared now !")
            Log.d(tag, "There are ${mapCoins.size} coins! " +
                    "The first one is ${mapCoins[0].currency} " +
                    "with value ${mapCoins[0].value}")
        }
    }

    fun checkNearestCoin(location: Location) : Coin? {

        var nearestCoin : Coin? = null
        val dists = ArrayList<Double>(mapCoins.size)
        mapCoins.stream().forEach { coin -> dists.add(coin.distToLocation(location)) }

        // Find the minimum distance's index
        val minDist = dists.min()
        if (minDist == null) {
            Log.d(tag, "No minimum distance! Something is wrong!")
        } else if (minDist <= 25){
            nearestCoin = mapCoins[dists.indexOf(minDist)]
            Log.d(tag, "The nearest coin is a ${nearestCoin.currency} of value ${nearestCoin.value}")
            Toast.makeText(this, "A ${nearestCoin.currency} is nearby!", Toast.LENGTH_SHORT).show()
        } else {
            Log.d(tag, "No coin can be collected now.")
        }
        return nearestCoin
    }

    override fun onBackPressed() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Are you sure!")
        alertDialog.setMessage("Do you want to return to the Login Page?")
        alertDialog.setPositiveButton("YES", { dialogInterface, i -> finish()})
        alertDialog.setNegativeButton("NO", {dialogInterface, i -> })
        alertDialog.show()
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if(mapboxMap == null) {
            Log.d(tag, "[onMapReady] mapboxMap is null")
        } else {
            map = mapboxMap
            // Set user interface options
            map?.uiSettings?.isCompassEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true
            // Make location information available
            enableLocation()

            exchangeRatesToday()
            initializeLocalMapToday()
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        // Present a toast or a dialog explaining why they need to grant access.
        Log.d(tag, "Permissions: $permissionsToExplain")
        Toast.makeText(this, "Need your location to play the game", Toast.LENGTH_SHORT)
    }

    override fun onPermissionResult(granted: Boolean) {
        Log.d(tag, "[onPermissionResult] granted == $granted")
        if (granted) {
            enableLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onLocationChanged(location: Location?) {

        if(location == null) { Log.d(tag, "[onLocationChanged] location is null")}

        location?.let{
            originLoaction = it
            setCameraPosition(it)
        }

        assert(mapCoins.size + collectedCoins.size == 50)

    }

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(tag, "[onConnected] requesting location updates")
        locationEngine?.requestLocationUpdates()
    }

    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            locationEngine?.requestLocationUpdates()
            locationLayerPlugin?.onStart()
        }
        mapView.onStart()

        // Set the collect button
        collectButton.setOnClickListener {

            // Ensure if there is a coin to collect
            val lastLocation = locationEngine?.lastLocation!!
            val nearestCoin = checkNearestCoin(lastLocation)
            if (nearestCoin == null) {
                Toast.makeText(this, "No coin nearby, try to be closer!", Toast.LENGTH_SHORT).show()
            } else {
                // collect coins
                val alertDialog = AlertDialog.Builder(this)
                alertDialog.setTitle("Found a new coin!")
                alertDialog.setMessage("Do you want to collect this coin?")
                alertDialog.setPositiveButton("YES") { _, _ ->
                    val marker = nearestCoin.marker
                    if (marker == null) {
                        Log.d(tag, "This coin has been removed on map, check!")
                    } else {
                        collectedCoins.add(nearestCoin)
                        mapCoins.remove(nearestCoin)
                        map.removeMarker(marker)
                        Toast.makeText(this, "Successfully collect a ${nearestCoin.currency}!", Toast.LENGTH_SHORT).show()
                    }
                }
                alertDialog.setNegativeButton("NO") { _, _ -> }
                alertDialog.show()
            }
        }

        // Set the pop-up menu button
        popupMenuButton.setOnClickListener {
            // pop-up menu
            Toast.makeText(this, "MENU!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        locationEngine?.removeLocationUpdates()
        locationLayerPlugin?.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        locationEngine?.deactivate()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        if(outState != null) {
            mapView.onSaveInstanceState(outState)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

}

