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

    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private lateinit var userID: String

    private val tag = "DataActivity"

    // data prepared for mapActivity
    private var firstDownloadToday: Boolean = true
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
        firstDownloadToday = intent.getBooleanExtra("firstDownloadToday", true)

        Log.d(tag, "FirstDownloadToady: $firstDownloadToday")
    }

    override fun onStart() {
        super.onStart()

        // New intent with the map (either fresh downloaded or obtained from firestore)
        val intent = Intent(this, MapActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // This is important for valid renewal of local wallet, i.e. remove expired coins
            putExtra("firstLaunchToday", firstDownloadToday)
        }

        if(firstDownloadToday) {
            mapToday = DownloadActivity.DownloadCompleteRunner.result
            if (mapToday.isNullOrEmpty()) {
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
            finish()
            startActivity(intent)

        } else {

            // Obtain the modified mapToday(jsonString) from firestore
            firestore?.collection("maps")
                    ?.document(userID)?.get()
                    ?.addOnSuccessListener { snapshot ->
                        val data = snapshot.data
                        if (data.isNullOrEmpty()) {
                            Log.d(tag, "No map stored! Check database!")
                            // Serious Error will happen if downloadDate has been updated whereas Map is not presented
                            // In this case, remove the downloadDate and let user try again
                            firestore?.collection("downloadDate")
                                    ?.document(userID)?.set(mapOf())
                                    ?.addOnSuccessListener {
                                        val alertDialog = AlertDialog.Builder(this)
                                        alertDialog.setTitle("Data is incorrect!")
                                        alertDialog.setMessage("Please return to the login and try again")
                                        alertDialog.setPositiveButton("YES") { _,_ ->
                                            val accidentalIntent = Intent(this, AuthenticationActivity::class.java)
                                            accidentalIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                            startActivity(accidentalIntent)
                                        }
                                        alertDialog.setNegativeButton("NO") {_,_ -> moveTaskToBack(true)}
                                        alertDialog.show()
                                    }
                                    ?.addOnFailureListener{
                                        Log.wtf(tag, it)
                                    }
                        } else {
                            Log.d(tag, "Restore map from the firestore!")
                            mapToday = data["mapToday"].toString().trim()
                            // Go to MapActivity with the wanted jsonString
                            intent.putExtra("mapToday", mapToday)
                            finish()
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
            startActivity(intent)}
        alertDialog.setNegativeButton("NO") {_,_ -> }
        alertDialog.show()
    }
}
