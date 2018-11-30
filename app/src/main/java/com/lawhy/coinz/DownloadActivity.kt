package com.lawhy.coinz

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class DownloadActivity : AppCompatActivity() {

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
        alertDialog.setPositiveButton("YES") { _,_ ->
            val intent = Intent(this, AuthenticationActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)}
        alertDialog.setNegativeButton("NO") {_,_ -> }
        alertDialog.show()
    }


    // read the download date from firestore, empty string if there is none
    private fun downloadIfFirst(){

        Log.d(tag, "Current user email: $userEmail")
        firestore?.collection("pool")
                ?.document("downloadDate")?.get()
                ?.addOnSuccessListener {
                    val data = it.data
                    if (data.isNullOrEmpty()) {
                        Log.d(tag, "No downloadDate data.")
                        firestore?.collection("pool")
                                ?.document("downloadDate")
                                ?.set(mapOf())
                    } else {
                        downloadDate = data[userID].toString().trim()
                    }
                    checkFirstDownloadToday()
                    // Customize an intent with extra information indicating first download or not
                    val intent = Intent(this, DataActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("firstDownloadToday", firstDownloadToday)
                    }
                    if(firstDownloadToday) {
                        // Download the map today and initialize everything
                        this.DownloadFileTask(DownloadCompleteRunner, intent)
                                .execute("http://homepages.inf.ed.ac.uk/stg/coinz/$currentDate/coinzmap.geojson")
                    } else {
                        // Restore data from firestore
                        finish()
                        startActivity(intent)
                    }
                }
    }

    private fun checkFirstDownloadToday(){

        Log.d(tag, "dd: $downloadDate")
        Log.d(tag, "cd: $currentDate")

        if (downloadDate != (currentDate)) {
            Log.i(tag, "First time to download today")
            downloadDate = currentDate
            firestore?.collection("pool")
                    ?.document("downloadDate")
                    ?.update(mapOf(userID to downloadDate))
                    ?.addOnSuccessListener {
                        Log.d("[DownloadDateUpdate]", downloadDate)
                    }
                    ?.addOnFailureListener{
                        Log.d("[DownloadDateUpdate]", "Failure! Go Check!")
                    }
        } else {
            firstDownloadToday = false
        }
    }

    interface DownloadCompleteListener {
        // Once the download complete, shift from curActivity to the nextActivity
        fun downloadComplete(result: String)
    }

    object DownloadCompleteRunner : DownloadCompleteListener {
        var result : String? = null
        override fun downloadComplete(result: String) {
            this.result = result
            Log.i("[DownloadMapToday]", "Completed!")
        }
    }

    inner class DownloadFileTask(private val caller: DownloadCompleteListener,
                                 private val intent: Intent) : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg urls: String): String = try{
            loadFileFromNetwork(urls[0])
        } catch (e: IOException){
            "Unable to load the content. Please check your network connection."
        }

        private fun loadFileFromNetwork(urlString: String): String {
            val stream : InputStream = downloadUrl(urlString)
            // read input from the stream, read the result as a string
            val result = stream.bufferedReader().use { it.readText() }
            stream.close()
            return result
        }

        // Given a string representation of a URL, sets up a connection and gets an input stream
        @Throws(IOException::class)
        fun downloadUrl(urlString: String): InputStream {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection

            conn.readTimeout = 10000
            conn.connectTimeout = 15000
            conn.requestMethod = "GET"
            conn.doInput = true
            conn.connect()
            return conn.inputStream
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            caller.downloadComplete(result)
            // Start a new task after completing the download
            startActivity(intent)
        }
    }


}
