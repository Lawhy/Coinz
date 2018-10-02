package com.lawhy.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_start.*
import kotlinx.android.synthetic.main.popup_demo.view.*
import android.view.WindowManager

class StartActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        // Popup Login Page
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
                popupWindow.width = ViewGroup.LayoutParams.WRAP_CONTENT
        popupWindow.height = ViewGroup.LayoutParams.WRAP_CONTENT

        //show
        popupWindow.showAtLocation(popupView,Gravity.CENTER,0, 0)

        // popupWindow.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        //dim the background
        dimBehind(popupWindow)

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
