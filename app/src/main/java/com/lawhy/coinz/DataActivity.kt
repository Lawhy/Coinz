package com.lawhy.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class DataActivity : AppCompatActivity() {

    /** This activity prepares necessary data before delivering the map,
     * the basic logic here is check *firstDownloadToday*,
     * if first, use the downloaded brand-new map and replace the one in fire-store;
     * else, retrieve the stored map from the fire-store: maps -> userEmail -> mapToday.
     * */

    // Fire-base
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private lateinit var userID: String
    private lateinit var userEmail: String

    private val tag = "DataActivity"

    // data prepared for mapActivity
    private var firstDownloadToday: Boolean = true
    private var lastDownloadDate: String = ""
    private var mapToday: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)

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

        // Modify the value of firstDownloadToday and lastDownloadDate according to the sent intent
        firstDownloadToday = intent.getBooleanExtra("firstDownloadToday", true)
        lastDownloadDate = intent.getStringExtra("lastDownloadDate")

        Log.d(tag, "FirstDownloadToady: $firstDownloadToday")
        Log.d(tag, "LastDownloadDate: $lastDownloadDate; Today: ${MyUtils().getCurrentDate()}")
    }

    override fun onStart() {
        super.onStart()

        // New intent with the map (either fresh downloaded or obtained from firestore)
        val intent = Intent(this, MapActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // This is important for valid renewal of local wallet, i.e. remove expired coins
            putExtra("firstLaunchToday", firstDownloadToday)
            putExtra("lastDownloadDate", lastDownloadDate)
        }

        if (firstDownloadToday) {
            mapToday = DownloadActivity.result
            if (mapToday.isNullOrEmpty()) {
                Log.d(tag, "No Coinz Map downloaded!")
            } else {
                Log.d(tag, "Coinz Map is prepared!")
            }

            // Replace the mapToday to the latest one
            firestore?.collection("maps")
                    ?.document(userEmail)
                    ?.set(mapOf("mapToday" to mapToday))
                    ?.addOnSuccessListener {
                        Log.d(tag, "Map today has been refreshed!")
                    }
                    ?.addOnFailureListener {
                        Log.w(tag, "Failure on map renewal! Go Check!", it)
                        Toast.makeText(this, "Something wrong happened!",
                                Toast.LENGTH_SHORT).show()
                    }

            // Renew the banked number today
            firestore?.collection("gold")
                    ?.document(userEmail)
                    ?.update(mapOf("bankedNumber" to 0))
                    ?.addOnSuccessListener {
                        Log.d(tag, "The banked number has been refreshed.")
                    }

            // Go to MapActivity with the wanted jsonString
            intent.putExtra("mapToday", mapToday)
            finish()
            startActivity(intent)

        } else {

            // Obtain the modified mapToday(jsonString) from fire-store
            firestore?.collection("maps")
                    ?.document(userEmail)?.get()
                    ?.addOnSuccessListener { snapshot ->
                        val data = snapshot.data
                        if (data == null || data.isEmpty()) {
                            Log.d(tag, "No map stored! Check database!")
                            // Serious Error will happen if downloadDate has been updated whereas Map is not presented
                            // In this case, remove the downloadDate and let user try again
                            firestore?.collection("pool")
                                    ?.document("downloadDate")?.update(mapOf(userID to ""))
                                    ?.addOnSuccessListener {
                                        val alertDialog = AlertDialog.Builder(this)
                                        alertDialog.setTitle("Data is incorrect!")
                                        alertDialog.setMessage("Please return to the login and try again")
                                        alertDialog.setPositiveButton("YES") { _, _ ->
                                            val accidentalIntent = Intent(this, AuthenticationActivity::class.java)
                                            accidentalIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                            startActivity(accidentalIntent)
                                        }
                                        alertDialog.setNegativeButton("NO") { _, _ -> moveTaskToBack(true) }
                                        alertDialog.show()
                                    }
                                    ?.addOnFailureListener {
                                        Log.wtf(tag, it)
                                    }
                        } else {
                            Log.d(tag, "Restore map from the fire-store!")
                            mapToday = data["mapToday"].toString().trim()
                            // Go to MapActivity with the wanted jsonString
                            intent.putExtra("mapToday", mapToday)
                            finish()
                            startActivity(intent)
                        }
                    }
                    ?.addOnFailureListener {
                        Log.w(tag, "Failure on map clean! Go Check!", it)
                        Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                    }
        }

    }

    // Go to authentication if back pressed.
    override fun onBackPressed() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Are you sure!")
        alertDialog.setMessage("Do you want to abandon the update?")
        alertDialog.setPositiveButton("YES") { _, _ ->
            val intent = Intent(this, AuthenticationActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
        alertDialog.setNegativeButton("NO") { _, _ -> }
        alertDialog.show()
    }
}
