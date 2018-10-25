package com.lawhy.coinz

import android.graphics.*
import android.graphics.drawable.Drawable
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.content.ContextCompat
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
import com.google.gson.JsonParser
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

    private var locationEngine : LocationEngine? = null
    private var locationLayerPlugin: LocationLayerPlugin? = null

    private val tag = "MapActivity"
    private var coins = ArrayList<HashMap<String, String>>() // [{"id":...,"currency": DOLR, "value":...}, ...]
    private var exchangeRates = HashMap<String, Double>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        Mapbox.getInstance(applicationContext, getString(R.string.access_token))

        mapView = findViewById(R.id.mapboxMapView)
        collectButton = findViewById(R.id.collectButton)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        collectButton.setOnClickListener {
            // collect coins
        }
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
                val jsonObject = JSONObject(jsonString)
                val rates = jsonObject.get("rates") as JSONObject

                // Update the Exchange Rates
                exchangeRates["SHIL"] = rates.getDouble("SHIL")
                exchangeRates["DOLR"] = rates.getDouble("DOLR")
                exchangeRates["QUID"] = rates.getDouble("QUID")
                exchangeRates["PENY"] = rates.getDouble("PENY")
                Log.d(tag, "Today's exchange rates: $rates")

                for (f in fc!!.iterator()) {


                    // Extract the properties
                    val properties:JsonObject? = f.properties()
                    if (properties == null) {
                        Log.d(tag, "Empty properties for the current feature.")
                        continue
                    }
                    val id = properties.get("id").asString
                    val value = properties.get("value").asString
                    val currency = properties.get("currency").asString
                    val symbol = properties.get("marker-symbol").asInt
                    val color = properties["marker-color"].asString
                    val dict = HashMap<String, String>(3)
                    dict["id"] = id
                    dict["currency"] = currency
                    dict["value"] = value
                    coins.add(dict)

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
                        map.addMarker(MarkerOptions()
                                .title(currency)
                                .snippet("Value: $value" )
                                .icon(icon)
                                .position(LatLng(point.latitude(), point.longitude())))
                    }
                }
                Log.d(tag, "Map is fully prepared now !")
                Log.d(tag, "Coins are: $coins")
            }
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

