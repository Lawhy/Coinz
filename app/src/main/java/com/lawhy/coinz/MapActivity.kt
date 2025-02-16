package com.lawhy.coinz

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
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
import net.danlew.android.joda.JodaTimeAndroid
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek

class MapActivity : AppCompatActivity(), PermissionsListener, LocationEngineListener, OnMapReadyCallback {

    /** This is the most important activity of all, the main functionality is:
     *     1. Display a map-box map of spread out coins according to the coin-z map.
     *     2. Manually collect coin if within 25 meters, if collected, update the map and the wallet.
     *     3. Auto-check of expired map (daily) or expired coins (weekly)
     *     4. Floating Action Button Menu that can lead the user to other activities including: (Order preserved)
     *         a. MyAccount: displaying statistics, managing coins.
     *         b. Social: friend and ranking list.
     *         c. BonusTrade: Key bonus feature of this app, with background story and more way to spend coins.
     *         d. Request: Deal with friend requests.
     *         e. Help: A help page specifying the app.
     * */

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private lateinit var permissionManager: PermissionsManager
    private lateinit var originLoaction: Location
    private lateinit var collectButton: Button

    // The FAB menu that lead to another activity
    private lateinit var fabMenu: FloatingActionButton
    private lateinit var fabMyAccount: FloatingActionButton
    private lateinit var fabFriendList: FloatingActionButton
    private lateinit var fabAlchemy: FloatingActionButton
    private lateinit var fabAddFriend: FloatingActionButton
    private lateinit var fabHelpPage: FloatingActionButton

    private var locationEngine: LocationEngine? = null
    private var locationLayerPlugin: LocationLayerPlugin? = null

    // Fire-base
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private lateinit var userID: String
    private lateinit var userEmail: String
    private var firestore: FirebaseFirestore? = null

    // Tags
    private val tag = "MapActivity"
    private val walletTag = "WALLET"

    private val translationY = -100f
    private var isMenuOpen = false
    private var simpleIndex = 0 // simple index for coins in the wallet
    // usage: keep coins with the same coinID
    private val interpolator = OvershootInterpolator()

    private var mapToday: String = "" // The map to be rendered

    /* A very important Boolean that has been passed through DownloadActivity, DataActivity
     * and now arrives MapActivity. It indicates if it is the first time to launch the app today,
     * if so, map should be re-downloaded, and on Monday coins should be removed from Wallet.
     * This Boolean should remain false after the app has been launched once within a day,
     * my resort here is to set it false in the renewalWallet() function which is invoked on start.
     * */
    private var firstTimeLaunch: Boolean = false
    private var lastDownloadDate: String = "" // Sometimes user does not play the game on Monday, this helps to check that, and ensure the expired coins are removed.

    private var coinsOnMap = ArrayList<Coin>() // Store the coins' (on the map) information
    private var collectedCoins = ArrayList<Coin>() // Store the *current* collected coins
    // This means it is temporary.
    private var exchangeRates = HashMap<String, Any>() // Store today's exchange rates

    // Initialise a map date variable to check if the date has changed
    private var mapDate = MyUtils().getCurrentDate()

    /* Override Functions
     * Below are functions that are overridden
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        Mapbox.getInstance(applicationContext, getString(R.string.access_token))

        mapView = findViewById(R.id.mapboxMapView)
        collectButton = findViewById(R.id.collectButton)
        fabMenu = findViewById(R.id.fab_menu)
        fabMyAccount = findViewById(R.id.fab_myAccount)
        fabFriendList = findViewById(R.id.fab_friendList)
        fabAlchemy = findViewById(R.id.fab_alchemy)
        fabAddFriend = findViewById(R.id.fab_addFriend)
        fabHelpPage = findViewById(R.id.fab_helpPage)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // Fire-base Initialization
        mAuth = FirebaseAuth.getInstance()
        user = mAuth?.currentUser
        userID = user!!.uid
        userEmail = user!!.email.orEmpty()
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings


        mapToday = intent.getStringExtra("mapToday")
        firstTimeLaunch = intent.getBooleanExtra("firstLaunchToday", false)
        lastDownloadDate = intent.getStringExtra("lastDownloadDate")
        // Obtain current map and firstTimeLaunch Info from DataActivity
        if (mapToday == "") {
            Log.w(tag, "No Coinz map detected!")
        }

        // Init the JodaTime which is very convenient for time check
        JodaTimeAndroid.init(this)
    }

    override fun onBackPressed() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Are you sure!")
        alertDialog.setMessage("Do you want to return to the login page?")
        alertDialog.setPositiveButton("YES") { _, _ ->
            mAuth?.signOut()
            val intent = Intent(this, AuthenticationActivity::class.java)
            finish()
            startActivity(intent)
        }
        alertDialog.setNegativeButton("NO") { _, _ -> }
        alertDialog.show()
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) {
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

        if (location == null) {
            Log.d(tag, "[onLocationChanged] location is null")
        }
        location?.let {
            originLoaction = it
            setCameraPosition(it)
        }

        // Check if the current map is expired, if so, download the new one
        if (mapDate != MyUtils().getCurrentDate()) {
            Log.wtf(tag, "Map has been expired!")
            val intent = Intent(this, DownloadActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_NEW_TASK)
            val alertOfMapExpired = AlertDialog.Builder(this)
            alertOfMapExpired.setTitle("Map has been expired!")
            alertOfMapExpired.setMessage("Click yes and download the new one.")
            alertOfMapExpired.setPositiveButton("YES") { _, _ ->
                startActivity(intent)
            }
            mapDate = MyUtils().getCurrentDate()
            alertOfMapExpired.show()
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
        mapView.onStart()
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            locationEngine?.requestLocationUpdates()
            locationLayerPlugin?.onStart()
        }
        mySetOnClick()
        renewWallet()
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
        locationEngine?.removeLocationEngineListener(this)
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
        if (outState != null) {
            mapView.onSaveInstanceState(outState)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    /* My Functions
     * The below are functions that are specially designed
     */

    private fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
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

        locationLayerPlugin = LocationLayerPlugin(mapView, map, locationEngine)
        locationLayerPlugin?.setLocationLayerEnabled(true)
        locationLayerPlugin?.cameraMode = CameraMode.TRACKING
        locationLayerPlugin?.renderMode = RenderMode.COMPASS
        lifecycle.addObserver(locationLayerPlugin!!)
    }

    private fun setCameraPosition(location: Location) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude), map.cameraPosition.zoom))
    }

    private fun exchangeRatesToday() {

        // Parse Geo-Json file and obtain the information we want
        val jsonObject = JSONObject(mapToday)
        val rates = jsonObject.get("rates") as JSONObject

        exchangeRates["SHIL"] = rates.getDouble("SHIL")
        exchangeRates["DOLR"] = rates.getDouble("DOLR")
        exchangeRates["QUID"] = rates.getDouble("QUID")
        exchangeRates["PENY"] = rates.getDouble("PENY")
        Log.i(tag, "Today's exchange rates: $rates")

    }

    private fun loadCoinzMap() {

        // Add Geo-json features on the map
        Log.i(tag, "Enable Coinz Map!")

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

        Log.i(tag, "Map is fully prepared now !")
        Log.i(tag, "There are ${coinsOnMap.size} coins! " +
                "The first one is ${coinsOnMap[0].currency} " +
                "with value ${coinsOnMap[0].value}")

    }

    private fun generateCoinOnMap(f: Feature): Coin? {

        var coin: Coin? = null

        // Extract the properties
        val properties: JsonObject? = f.properties()
        if (properties == null) {
            Log.w(tag, "[MapGeneration] Empty properties for the current feature.")
            return null
        }

        val id = properties.get("id").asString
        val value = properties.get("value").asDouble
        val currency = properties.get("currency").asString
        val symbol = properties.get("marker-symbol").asInt
        val color = properties["marker-color"].asString

        // Using the MyUtils class methods to combine color and number on the same icon
        val icMarker = getDrawable(R.drawable.ic_roundmarker)
        val colorFilter = LightingColorFilter(Color.parseColor(color), Color.parseColor(color))
        icMarker?.colorFilter = colorFilter

        val icNumber: Drawable? = MyUtils().symbolDrawable(this, symbol)
        val combinedDrawable = MyUtils().combineDrawable(icMarker, icNumber)
        val icon = MyUtils().drawableToIcon(this, combinedDrawable)

        // Extract the geometric information and add marker and coin
        val point = f.geometry()
        if (point is Point) {

            // add marker
            val pos = LatLng(point.latitude(), point.longitude())
            val marker = map.addMarker(MarkerOptions()
                    .title(currency)
                    .snippet("Value: %.3f".format(value))
                    .icon(icon)
                    .position(pos))

            coin = Coin(id, currency, value, marker) // Parse the information into a coin object
        }

        return coin
    }

    private fun checkNearestCoin(location: Location): Coin? {

        var nearestCoin: Coin? = null
        val dists = ArrayList<Double>(coinsOnMap.size)
        coinsOnMap.stream().forEach { coin -> dists.add(coin.distToLocation(location)) }

        // Find the minimum distance's index
        val minDist = dists.min()
        when {
            minDist == null -> Log.w(tag, "No minimum distance! Something is wrong!")
            minDist <= 25 -> {
                nearestCoin = coinsOnMap[dists.indexOf(minDist)]
                Log.i(tag, "The nearest coin is a ${nearestCoin.currency} of value ${nearestCoin.value}")
                Toast.makeText(this, "A ${nearestCoin.currency} is nearby!", Toast.LENGTH_SHORT).show()
            }
            else -> Log.i(tag, "No coin can be collected now.")
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
            updateWallet(nearestCoin)
            Toast.makeText(this, "Successfully collect a ${nearestCoin.currency}!", Toast.LENGTH_SHORT).show()
            Log.i(tag, "${collectedCoins.size} collected!")
            Log.i(tag, "${coinsOnMap.size} remaining!")
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
            val feature = features.get(i - 1) as JSONObject
            val properties = feature.get("properties") as JSONObject
            val id = collectedCoin.id
            val currency = collectedCoin.currency
            val value = collectedCoin.value
            if (properties.get("id") as String == id
                    && properties.get("currency") as String == currency
                    && properties.get("value") as String == value.toString()) {
                Log.d(tag, "Coin: [$currency] of [$value] is about to be removed from mapToday.")
                removeIndex = i - 1
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
                ?.document(userEmail)
                ?.set(mapOf("mapToday" to mapToday))
                ?.addOnSuccessListener {
                    Log.d(tag, "Map Today has been modified!")
                }
                ?.addOnFailureListener {
                    Log.d(tag, "Fail to upload changes on map!", it)
                }
    }

    // Remove expired coins on every Monday First Launch
    private fun renewWallet() {

        // Parameters that check whether we need to renew the wallet, i.e. remove expired coins
        val day = LocalDate.now().dayOfWeek.name
        Log.d(walletTag, "Today is $day, first time launch: $firstTimeLaunch")
        // Give a fair warning of coins about to be expired on the weekend
        if (day == "SATURDAY" || day == "SUNDAY") {
            Toast.makeText(this, "It's $day now! Use your coins!", Toast.LENGTH_SHORT).show()
        }

        // Check null value for brand new account.
        if (lastDownloadDate == "null" || lastDownloadDate == "") {
            Log.d(tag, "Brand new account.")
            firstTimeLaunch = false // set this boolean to false to prevent overlapped renewal
            return
        }

        // Checking if the last download date is before this week's Monday, if so, clean the coins
        val parsedDate = LocalDate.parse(lastDownloadDate, DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        val dateNow = LocalDate.now()
        val monday = dateNow.with(DayOfWeek.MONDAY)
        val lastDownloadBeforeMonday = parsedDate.isBefore(monday)
        Log.i(tag, "lastDownloadBeforeMonday: $lastDownloadBeforeMonday")
        Log.d(tag, "Monday: ${monday.dayOfYear} LastDownloadDate: ${parsedDate.dayOfYear}")

        if (lastDownloadBeforeMonday && firstTimeLaunch) {
            firestore?.collection("coins")
                    ?.document(userEmail)?.set(mapOf())
                    ?.addOnSuccessListener { Toast.makeText(this, "It's $day now! Unused Coins have been expired", Toast.LENGTH_SHORT).show() }
                    ?.addOnFailureListener { Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show() }
            firestore?.collection("foreignCoins")
                    ?.document(userEmail)?.set(mapOf())
                    ?.addOnSuccessListener { Toast.makeText(this, "It's $day now! Unused Coins have been expired", Toast.LENGTH_SHORT).show() }
                    ?.addOnFailureListener { Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show() }
            Log.d(tag, "The coins are all expired!")
        }
        firstTimeLaunch = false // set this boolean to false to prevent overlapped renewal
    }

    private fun updateWallet(coin: Coin) {

        val coinMap = HashMap<String, Any>()
        coinMap["id"] = coin.id
        coinMap["currency"] = coin.currency
        coinMap["value"] = coin.value

        val coinsDocRef = firestore?.collection("coins")?.document(userEmail)
        coinsDocRef
                ?.get()
                ?.addOnSuccessListener {
                    val data = it.data
                    if (data == null || data.isEmpty()) {
                        simpleIndex = 0
                        coinsDocRef.set(mapOf())
                    } else {
                        simpleIndex = data.size
                    }
                    coinsDocRef
                            .update(mapOf("$simpleIndex" to coinMap))
                            .addOnSuccessListener { Log.i(walletTag, "${simpleIndex}th coin has been added") }
                }

    }

    private fun mySetOnClick() {
        // Set the collect button
        collectButton.setOnClickListener {

            // Ensure if there is a coin to collect
            val lastLocation = locationEngine?.lastLocation
            if (lastLocation == null) {
                Log.wtf(tag, "No Location At All.")
                Toast.makeText(this, "Please set the location for the emulator...", Toast.LENGTH_SHORT).show()
            } else {
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
        }

        // Enable the Fab Menu
        initFabMenu()
    }

    /*
     * Functions related to the Floating Action Button Menu that leads to another activity
     * 1. initFabMenu: initialise button animation and set on-click listener
     * 2. openMenu: Add animation effect on menu open
     * 3. closeMenu: Add animation effect on menu closed
     */

    private fun initFabMenu() {

        // The three sub-buttons are invisible initially
        fabMyAccount.visibility = GONE
        fabFriendList.visibility = GONE
        fabAlchemy.visibility = GONE
        fabAddFriend.visibility = GONE
        fabHelpPage.visibility = GONE
        // Set alpha to 0f for animation effect latter
        fabMyAccount.alpha = 0f
        fabFriendList.alpha = 0f
        fabAlchemy.alpha = 0f
        fabAddFriend.alpha = 0f
        fabHelpPage.alpha = 0f

        fabMenu.setOnClickListener {
            Log.i(tag, "onClick: fab Menu")
            if (isMenuOpen) {
                closeMenu()
            } else {
                openMenu()
            }
        }
        fabMyAccount.setOnClickListener {
            Log.i(tag, "onClick: fab -> MyAccount")
            closeMenu()
            val intent = Intent(this, MyAccountActivity::class.java)
            intent.putExtra("exchangeRates", exchangeRates)
            startActivity(intent)
        }
        fabFriendList.setOnClickListener {
            Log.i(tag, "onClick: fab -> FriendList")
            closeMenu()
            startActivity(Intent(this, SocialActivity::class.java))
        }
        fabAlchemy.setOnClickListener {
            Log.i(tag, "onClick: fab -> Alchemy")
            closeMenu()
            val intent = Intent(this, AlchemyActivity::class.java)
            var highestXRate = 0.0  // This is one key component of Alchemy play mode
            for (rate in exchangeRates.values) {
                val rateValue = rate.toString().toDouble()
                if (rateValue > highestXRate) {
                    highestXRate = rateValue
                }
            }
            intent.putExtra("highestXRate", highestXRate)
            startActivity(intent)
        }
        fabAddFriend.setOnClickListener {
            Log.i(tag, "onClick: fab -> addFriend")
            closeMenu()
            startActivity(Intent(this, RequestActivity::class.java))
        }
        fabHelpPage.setOnClickListener {
            Log.i(tag, "onClick: fab -> helpPage")
            closeMenu()
            startActivity(Intent(this, HelpPageActivity::class.java))
        }

    }

    private fun openMenu() {
        isMenuOpen = !isMenuOpen

        Log.i(tag, "fab Menu is open!")
        fabMenu.animate()
                .setInterpolator(interpolator)
                .rotationBy(45f)
                .setDuration(300)
                .start()

        fabMyAccount.visibility = View.VISIBLE
        fabFriendList.visibility = View.VISIBLE
        fabAlchemy.visibility = View.VISIBLE
        fabAddFriend.visibility = View.VISIBLE
        fabHelpPage.visibility = View.VISIBLE

        fabMyAccount.animate()
                .translationY(0f)
                .alpha(1f)
                .setInterpolator(interpolator)
                .setDuration(300)
                .start()

        fabFriendList.animate()
                .translationY(0f)
                .alpha(1f)
                .setInterpolator(interpolator)
                .setDuration(300)
                .start()

        fabAlchemy.animate()
                .translationY(0f)
                .alpha(1f)
                .setInterpolator(interpolator)
                .setDuration(300)
                .start()

        fabAddFriend.animate()
                .translationY(0f)
                .alpha(1f)
                .setInterpolator(interpolator)
                .setDuration(300)
                .start()


        fabHelpPage.animate()
                .translationY(0f)
                .alpha(1f)
                .setInterpolator(interpolator)
                .setDuration(300)
                .start()

    }

    private fun closeMenu() {
        isMenuOpen = !isMenuOpen

        Log.i(tag, "fab Menu is closed!")
        fabMenu.animate()
                .setInterpolator(interpolator)
                .rotationBy(45f)
                .setDuration(300)
                .start()

        fabMyAccount.animate()
                .translationY(translationY)
                .alpha(0f)
                .setInterpolator(interpolator)
                .setDuration(300)
                .start()

        fabFriendList.animate()
                .translationY(translationY)
                .alpha(0f)
                .setInterpolator(interpolator)
                .setDuration(300)
                .start()

        fabAlchemy.animate()
                .translationY(translationY)
                .alpha(0f)
                .setInterpolator(interpolator)
                .setDuration(300)
                .start()

        fabAddFriend.animate()
                .translationY(translationY)
                .alpha(0f)
                .setInterpolator(interpolator)
                .setDuration(300)
                .start()

        fabHelpPage.animate()
                .translationY(translationY)
                .alpha(0f)
                .setInterpolator(interpolator)
                .setDuration(300)
                .start()

        fabMyAccount.visibility = GONE
        fabFriendList.visibility = GONE
        fabAlchemy.visibility = GONE
        fabAddFriend.visibility = GONE
        fabHelpPage.visibility = GONE
    }


}

