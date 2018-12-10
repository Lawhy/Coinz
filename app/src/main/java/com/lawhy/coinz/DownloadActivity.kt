package com.lawhy.coinz

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings


class DownloadActivity : AppCompatActivity(), DownloadCompleteListener {

    /** This activity provides downloading of new map if ${firstDownloadToday} is true.
     * ${firstDownloadToday} will be checked by the downloadDate taken from fire-store:
     * (PATH) pool -> downloadDate -> userID : the last download date.
     * Next activity will be launched only when download is complete or ${firstDownloadToday} is false.
     * */

    // Fire-base
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private lateinit var userID: String
    private lateinit var userEmail: String

    private val tag = "DownloadActivity"

    // Format: YYYY/MM/DD
    private lateinit var currentDate: String
    private lateinit var downloadDate: String

    // Check first download today
    private var firstDownloadToday = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        // Firebase Initialization
        mAuth = FirebaseAuth.getInstance()
        user = mAuth?.currentUser
        userID = user!!.uid
        userEmail = user!!.email.orEmpty()
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore?.firestoreSettings = settings

    }

    override fun onStart() {
        super.onStart()
        currentDate = MyUtils().getCurrentDate()
        downloadDate = ""
        downloadIfFirst() // download the map if it is the first time today
    }

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

    // Download Result that will be sent to Data Activity
    companion object {
        var result: String? = null
    }

    override fun downloadComplete(result: String, intent: Intent) {
        DownloadActivity.result = result
        Log.i("[DownloadMapToday]", "Completed!")
        // Start a new task after completing the download
        startActivity(intent)
    }


    // read the download date from firestore, empty string if there is none
    private fun downloadIfFirst() {

        Log.d(tag, "Current user email: $userEmail")
        firestore?.collection("pool")
                ?.document("downloadDate")?.get()
                ?.addOnSuccessListener {
                    val data = it.data
                    if (data == null || data.isEmpty()) {
                        Log.d(tag, "No downloadDate data.")
                        firestore?.collection("pool")
                                ?.document("downloadDate")
                                ?.set(mapOf())
                    } else {
                        downloadDate = data[userID].toString().trim()
                    }
                    val lastDownloadDate: String = checkFirstDownloadToday()
                    // Customize an intent with extra information indicating first download or not
                    val intent = Intent(this, DataActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("firstDownloadToday", firstDownloadToday)
                        putExtra("lastDownloadDate", lastDownloadDate)
                    }
                    if (firstDownloadToday) {
                        // Download the map today and initialize everything
                        DownloadFileTask(this, intent)
                                .execute("http://homepages.inf.ed.ac.uk/stg/coinz/$currentDate/coinzmap.geojson")
                    } else {
                        // Restore data from firestore
                        finish()
                        startActivity(intent)
                    }
                }
    }

    private fun checkFirstDownloadToday(): String {

        Log.d(tag, "dd: $downloadDate")
        Log.d(tag, "cd: $currentDate")

        if (downloadDate != (currentDate)) {
            Log.i(tag, "First time to download today")
            firestore?.collection("pool")
                    ?.document("downloadDate")
                    ?.update(mapOf(userID to currentDate))
                    ?.addOnSuccessListener {
                        Log.d("[DownloadDateUpdate]", currentDate)
                    }
                    ?.addOnFailureListener {
                        Log.d("[DownloadDateUpdate]", "Failure! Go Check!")
                    }
        } else {
            firstDownloadToday = false
        }
        return downloadDate  // Now the download date becomes the last download date which is needed for checking expiration of coins
    }

}
