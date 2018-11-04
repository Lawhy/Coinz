package com.lawhy.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.support.v7.app.AlertDialog
import kotlinx.android.synthetic.main.activity_start.*


class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        floatingActionButton_login.setOnClickListener {
            startActivity(Intent(this, AuthenticationActivity::class.java))
        }

    }


    override fun onBackPressed() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Are you sure!")
        alertDialog.setMessage("Do you want to exit the game?")
        alertDialog.setPositiveButton("YES") { _,_ -> finish()}
        alertDialog.setNegativeButton("NO") {_,_ -> }
        alertDialog.show()
    }

}

