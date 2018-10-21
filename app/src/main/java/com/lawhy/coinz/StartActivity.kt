package com.lawhy.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_start.*
import kotlinx.android.synthetic.main.popup_demo.view.*
import android.view.WindowManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class StartActivity : AppCompatActivity() {

    private val tag = "StartActivity"

    private var downloadDate = "" // Format: YYYY/MM/DD
    private val preferencesFile = "MyPrefsFile" // for storing preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        // Popup Login Page
        val popupWindow = PopupWindow(this)
        floatingActionButton_login.setOnClickListener {
            popupLogin(popupWindow)
        }

    }

    override fun onStart() {
        super.onStart()

        // Restore preferences
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)

        // use "" as the default value (this might be the first time the app is run)
        downloadDate = settings.getString("lastDownloadDate", "")

        // Write a message to "logcat" (for debugging purposes)
        Log.d(tag, "[onStart] Recalled lastDownloadDate is '$downloadDate'" )

        // Download today's JSON map
        var current = LocalDateTime.now()
        val formatter =  DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val current_date = current.format(formatter)
        Log.i("CurrentDate", "$current_date")
        DownloadFileTask(DownloadCompleteRunner).execute("http://homepages.inf.ed.ac.uk/stg/coinz/$current_date/coinzmap.geojson")
        downloadDate = current_date
    }

    override fun onStop() {
        super.onStop()
        Log.d(tag, "[onStop] Storing lastDownloadDate of $downloadDate")

        // All objects are from android.context.Context
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)

        // We need an Editor object to make preference changes.
        val editor = settings.edit()
        editor.putString("lastDownloadDate", downloadDate)
        // Apply the edits
        editor.apply()
    }

    private fun popupLogin(popupWindow: PopupWindow) {
        val popupView = View.inflate(this, R.layout.popup_demo, null)

        // set click listener for login button
        popupView.closePopupBtn.setOnClickListener { _ ->
            popupWindow.dismiss()
        }

        popupWindow.contentView = popupView
                popupWindow.width = ViewGroup.LayoutParams.WRAP_CONTENT
        popupWindow.height = ViewGroup.LayoutParams.WRAP_CONTENT

        //show
        popupWindow.showAtLocation(popupView,Gravity.CENTER,0, 0)

        //dim the background
        dimBehind(popupWindow)

        popupWindow.isFocusable = true
        popupWindow.update()

        //get Login Information
        popupView.inBtn.setOnClickListener { _ ->
            val emailString = popupView.emailText.getText().toString().trim()
            val passwordString = popupView.passwordText.getText().toString().trim()

            //testing
            toMapbox()

            when {
                emailString.isEmpty() or passwordString.isEmpty() -> {
                    Toast.makeText(this, "Type Something My Friend.", Toast.LENGTH_SHORT).show()
                }
                emailString.equals("lawhy") and passwordString.equals("admin") -> {
                    Toast.makeText(this, "Hi, Boss!", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Check Any Typos, Old Sport!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun toMapbox() {
        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)
    }

    private fun dimBehind(popupWindow: PopupWindow) {
        val container = popupWindow.contentView.rootView
        val context = popupWindow.contentView.context
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val p = container.layoutParams as WindowManager.LayoutParams
        p.flags = p.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        p.dimAmount = 0.4f
        wm.updateViewLayout(container, p)
    }



}
