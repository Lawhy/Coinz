package com.lawhy.coinz

import android.content.Intent
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.drawable.Drawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import org.json.JSONObject

class DataActivity : AppCompatActivity() {

    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private lateinit var userID: String

    private var firstDownloadToday: Boolean = true

    private val tag = "DataActivity"

    // data prepared for mapActivity
    private var mapToday: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)

        // Firebase Initialization
        mAuth = FirebaseAuth.getInstance()
        user = mAuth?.currentUser
        userID = user!!.uid
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings

        // Modify the value of firstDownloadToday according to the sent intent
        val extras = intent.extras
        firstDownloadToday = extras["firstDownloadToday"] as Boolean

        Log.d(tag, "FirstDownloadToady: $firstDownloadToday")
    }

    override fun onStart() {
        super.onStart()

        // New intent with the map (either fresh downloaded or obtained from firestore)
        val intent = Intent(this, MapActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if(firstDownloadToday) {
            mapToday = DownloadActivity.DownloadCompleteRunner.result
            if (mapToday == null) {
                Log.d(tag, "No Coinz Map downloaded!")
            } else {
                Log.d(tag, "Coinz Map is prepared!")
            }

            // Replace the mapToday to the latest one
            firestore?.collection("maps")
                    ?.document(userID)
                    ?.set(mapOf("mapToday" to mapToday))
                    ?.addOnSuccessListener {
                        Log.d(tag, "Map today has been refreshed!")
                    }
                    ?.addOnFailureListener{
                        Log.w(tag, "Failure on map renewal! Go Check!", it)
                        Toast.makeText(this, "Something wrong happened!",
                                Toast.LENGTH_SHORT).show()
                    }

            // Go to MapActivity with the wanted jsonString
            intent.putExtra("mapToday", mapToday)
            startActivity(intent)

        } else {

            // Obtain the modified mapToday(jsonString) from firestore
            firestore?.collection("maps")
                    ?.document(userID)?.get()
                    ?.addOnSuccessListener {
                        val data = it.data
                        if (data == null) {
                            Log.d(tag, "No map stored! Check database!")
                        } else {
                            Log.d(tag, "Restore map from the firestore!")
                            mapToday = data["mapToday"].toString().trim()
                            // Go to MapActivity with the wanted jsonString
                            intent.putExtra("mapToday", mapToday)
                            startActivity(intent)
                        }
                    }
                    ?.addOnFailureListener{
                        Log.w(tag, "Failure on map clean! Go Check!", it)
                        Toast.makeText(this, "Something wrong happened!",
                                Toast.LENGTH_SHORT).show()
                    }
        }

    }


    override fun onBackPressed() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Are you sure!")
        alertDialog.setMessage("Do you want to abandon the update?")
        alertDialog.setPositiveButton("YES") { _,_ ->
            val intent = Intent(this, AuthenticationActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(Intent(this, AuthenticationActivity::class.java))}
        alertDialog.setNegativeButton("NO") {_,_ -> }
        alertDialog.show()
    }
}
