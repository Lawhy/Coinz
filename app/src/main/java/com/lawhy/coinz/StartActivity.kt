package com.lawhy.coinz


import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Context
import android.support.design.widget.Snackbar
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_start.*
import kotlinx.android.synthetic.main.popup_demo.view.*


class StartActivity : AppCompatActivity() {




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val popupWindow = PopupWindow(this)
        floatingActionButton_login.setOnClickListener {
            popupLogin(popupWindow)
        }

    }


    private fun popupLogin(popupWindow: PopupWindow) {
        val popupView = View.inflate(this, R.layout.popup_demo, null)

        // set click listener for login button
        popupView.closePopupBtn.setOnClickListener { _ ->
            popupWindow.dismiss()
        }

        popupWindow.contentView = popupView
        popupWindow.width = ViewGroup.LayoutParams.MATCH_PARENT
        popupWindow.height = ViewGroup.LayoutParams.WRAP_CONTENT

        //show
        popupWindow.showAtLocation(popupView,Gravity.CENTER,0, 0)

        popupWindow.isFocusable = true
        popupWindow.update()

        //get Login Information
        popupView.inBtn.setOnClickListener { _ ->
            val emailString = popupView.emailText.getText().toString().trim()
            val passwordString = popupView.passwordText.getText().toString().trim()

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

//    // function to hide keyboard
//    fun hideKeyboard() {
//        try {
//            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
//        } catch (e: Exception) {
//            // TODO: handle exception
//        }
//    }

}
