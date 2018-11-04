package com.lawhy.coinz

import android.content.Intent
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.gson.JsonArray
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
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.style.layers.LineLayer
import org.json.JSONArray
import org.json.JSONObject

class MapActivity : AppCompatActivity(), PermissionsListener, LocationEngineListener, OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private lateinit var permissionManager: PermissionsManager
    private lateinit var originLoaction: Location
    private lateinit var collectButton: Button
    private lateinit var popupMenuButton: FloatingActionButton

    private var locationEngine : LocationEngine? = null
    private var locationLayerPlugin: LocationLayerPlugin? = null
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private lateinit var userID: String
    private var firestore: FirebaseFirestore? = null

    private val tag = "MapActivity"
    private var mapToday: String = ""
    private var coinsOnMap = ArrayList<Coin>() // Store the coins' (on the map) information
    private var collectedCoins = ArrayList<Coin>() // Store the current collected coins
    private var exchangeRates = HashMap<String, Any>()

    /* Override Functions
    * The below are functions that are overridden
    * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        Mapbox.getInstance(applicationContext, getString(R.string.access_token))

        mapView = findViewById(R.id.mapboxMapView)
        collectButton = findViewById(R.id.collectButton)
        popupMenuButton = findViewById(R.id.popupMenuButton)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // Firebase Initialization
        mAuth = FirebaseAuth.getInstance()
        user = mAuth?.currentUser
        userID = user!!.uid
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings

        // Obtain current map from DataActivity
        mapToday = intent.extras["mapToday"] as String
        if(mapToday == "") {
            Log.d(tag, "No Coinz map detected!")
        }

    }

    override fun onBackPressed() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Are you sure!")
        alertDialog.setMessage("Do you want to return to the login page?")
        alertDialog.setPositiveButton("YES") { _,_ ->
            val intent = Intent(this, AuthenticationActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(Intent(this, AuthenticationActivity::class.java))}
        alertDialog.setNegativeButton("NO") {_,_ -> }
        alertDialog.show()
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if(mapboxMap == null) {
            Log.d(tag, "[onMapReady] mapboxMap is null")
        } else {
            map = mapboxMap
            // Set user interface options
            map.uiSettings.isCompassEnabled = true
            map.uiSettings.isZoomControlsEnabled = true
            // Make location information available
            enableLocation()

            // If this is the first time opening the map today
            exchangeRatesToday()
            loadCoinzMap()
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
        mySetOnClick()
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

    /* My Functions
   * The below are functions that are specially designed
   * */

    private fun enableLocation() {
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

    private fun exchangeRatesToday() {

        // Parse Geo-Json file and obtain the information we want
        val jsonObject = JSONObject(mapToday)
        val rates = jsonObject.get("rates") as JSONObject

        exchangeRates["SHIL"] = rates.getDouble("SHIL")
        exchangeRates["DOLR"] = rates.getDouble("DOLR")
        exchangeRates["QUID"] = rates.getDouble("QUID")
        exchangeRates["PENY"] = rates.getDouble("PENY")
        Log.d(tag, "Today's exchange rates: $rates")

    }

    private fun loadCoinzMap( ) {

        // Add Geo-json features on the map
        Log.d(tag, "Enable Coinz Map!")

        val jsonSource = GeoJsonSource("geojson", mapToday)
        map.addSource(jsonSource)
        map.addLayer(LineLayer("geojson", "geojson"))

        // Parse each feature and generate coin correspondingly
        val fc: List<Feature>? = FeatureCollection.fromJson(mapToday).features()
        for (f in fc!!.iterator()) {
            val coin = generateCoinOnMap(f)
            if (coin != null) {
                coinsOnMap.add(coin)
            }
        }

        Log.d(tag, "Map is fully prepared now !")
        Log.d(tag, "There are ${coinsOnMap.size} coins! " +
                "The first one is ${coinsOnMap[0].currency} " +
                "with value ${coinsOnMap[0].value}")

    }

    private fun generateCoinOnMap(f: Feature): Coin?{

        var coin: Coin? = null

        // Extract the properties
        val properties:JsonObject? = f.properties()
        if (properties == null) {
            Log.d(tag, "Empty properties for the current feature.")
            return null
        }

        val id = properties.get("id").asString
        val value = properties.get("value").asDouble
        val currency = properties.get("currency").asString
        val symbol = properties.get("marker-symbol").asInt
        val color = properties["marker-color"].asString

        // Using the IconUtils class methods to combine color and number on the same icon
        val icMarker = ContextCompat.getDrawable(this, R.drawable.ic_roundmarker)
        val colorFilter = LightingColorFilter(Color.parseColor(color), Color.parseColor(color))
        icMarker?.colorFilter = colorFilter

        val icNumber : Drawable? = MyUtil().symbolDrawable(this, symbol)
        val combinedDrawable = IconUtils.combineDrawable(icMarker, icNumber)
        val icon = IconUtils.drawableToIcon(this, combinedDrawable)

        // Extract the geometric information and add marker and coin
        val point = f.geometry()
        if (point is Point) {

            // add marker
            val pos = LatLng(point.latitude(), point.longitude())
            val marker = map.addMarker(MarkerOptions()
                    .title(currency)
                    .snippet("Value: $value" )
                    .icon(icon)
                    .position(pos))

            coin = Coin(id, currency, value, marker) // Parse the information into a coin object
        }

        return coin
    }

    private fun checkNearestCoin(location: Location) : Coin? {

        var nearestCoin : Coin? = null
        val dists = ArrayList<Double>(coinsOnMap.size)
        coinsOnMap.stream().forEach { coin -> dists.add(coin.distToLocation(location)) }

        // Find the minimum distance's index
        val minDist = dists.min()
        if (minDist == null) {
            Log.d(tag, "No minimum distance! Something is wrong!")
        } else if (minDist <= 25){
            nearestCoin = coinsOnMap[dists.indexOf(minDist)]
            Log.d(tag, "The nearest coin is a ${nearestCoin.currency} of value ${nearestCoin.value}")
            Toast.makeText(this, "A ${nearestCoin.currency} is nearby!", Toast.LENGTH_SHORT).show()
        } else {
            Log.d(tag, "No coin can be collected now.")
        }
        return nearestCoin
    }

    private fun collectNearestCoin(nearestCoin: Coin) {

        val marker = nearestCoin.marker
        if (marker == null) {
            Log.d(tag, "This coin has been removed on map, check!")
        } else {
            collectedCoins.add(nearestCoin)
            coinsOnMap.remove(nearestCoin)
            map.removeMarker(marker)
            updateCoinzMap(nearestCoin)
            Toast.makeText(this, "Successfully collect a ${nearestCoin.currency}!", Toast.LENGTH_SHORT).show()
            Log.d(tag, "${collectedCoins.size} collected!")
            Log.d(tag, "${coinsOnMap.size} remaining!")
        }

    }

    private fun updateCoinzMap(collectedCoin: Coin) {

        // Delete collected coins from the jsonString
        val jobj = JSONObject(mapToday)
        val len1 = jobj.toString().length
        val features = jobj.get("features") as JSONArray
        var removeIndex = -1

        // Find the index of the feature that contains the collected coin's information
        for (i in 1..features.length()) {
            val feature = features.get(i-1) as JSONObject
            val properties = feature.get("properties") as JSONObject
            val id = collectedCoin.id
            val currency = collectedCoin.currency
            val value = collectedCoin.value
            if (properties.get("id") as String == id
                    && properties.get("currency") as String == currency
                    && properties.get("value") as String == value.toString()) {
                Log.d(tag, "Coin: [$currency] of [$value] is about to be removed from mapToday.")
                removeIndex = i-1
                break
            }
        }

        // Remove the corresponding coin from the JSON string and update mapToday
        features.remove(removeIndex)
        jobj.put("features", features)
        Log.d(tag, "Length Difference after Removal: ${len1 - jobj.toString().length}")
        mapToday = jobj.toString()

        // upload the modified map to firestore
        firestore?.collection("maps")
                ?.document(userID)
                ?.set(mapOf("mapToday" to  mapToday))
                ?.addOnSuccessListener {
                    Log.d(tag, "Map Today has been modified!")
                }
                ?.addOnFailureListener {
                    Log.d(tag, "Fail to upload changes on map!", it)
                }
    }

    private fun mySetOnClick() {
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
                    collectNearestCoin(nearestCoin)
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


}

