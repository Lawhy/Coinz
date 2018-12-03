package com.lawhy.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button


class HelpPageActivity : AppCompatActivity() {

    /** Providing help page specifying the buttons' functionality.
     * */


    private lateinit var helpPageGetItBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_page)

        helpPageGetItBtn = findViewById(R.id.helpPageGetItBtn)
        helpPageGetItBtn.setOnClickListener {
            finish()
        }
    }
}
