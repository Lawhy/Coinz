package com.lawhy.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth

class AuthenticationActivity : AppCompatActivity(){

    private lateinit var emailText: EditText
    private lateinit var passwordText: EditText
    private lateinit var progressBar: ProgressBar
    private var mAuth: FirebaseAuth? = null
    private val tag = "AuthenticationActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        // Initialise Views
        emailText = findViewById(R.id.AutEmail)
        passwordText = findViewById(R.id.AutPassword)
        progressBar = findViewById(R.id.progressLogin)
        // Fire-base components
        mAuth = FirebaseAuth.getInstance()
        mySetOnClick()
    }

    override fun onBackPressed() {
        finishAndRemoveTask()
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun mySetOnClick(){

        // SignInBtn
        val signInBtn: Button = findViewById(R.id.AutSignInBtn)
        signInBtn.setOnClickListener { userLoginIn() }

        // SignUpBtn
        val signUpBtn: Button = findViewById(R.id.AutSignUpBtn)
        signUpBtn.setOnClickListener { startActivity(Intent(this, SignUpActivity::class.java)) }

        // CloseBtn
        val closeBtn: ImageButton = findViewById(R.id.closeAutBtn)
        closeBtn.setOnClickListener {
            finish()
            startActivity(Intent(this, MainActivity::class.java)) }
    }

    private fun userLoginIn() {

        val email = emailText.text.toString().trim()
        val password = passwordText.text.toString().trim()
        if (!checkValidEmailPassword(email, password)) return // return if not valid
        progressBar.visibility = View.VISIBLE

        mAuth?.signInWithEmailAndPassword(email, password)?.addOnCompleteListener { task ->
            progressBar.visibility = View.GONE
            if(task.isSuccessful){
                Log.d(tag, "Successfully log in!")
                // Go to next activity for successful login
                val intent = Intent(this, DownloadActivity::class.java)
                finish()
                startActivity(intent)
            } else {
                Toast.makeText(this, task.exception?.message,
                        Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun checkValidEmailPassword(email: String, password: String): Boolean {
        // Handle the empty strings & wrong format cases
        if (email.isEmpty()) {
            emailText.error = "Email is required!"
            emailText.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailText.error = "Please enter a valid email"
            emailText.requestFocus()
            return false
        }

        if(password.isEmpty()) {
            passwordText.error = "Password is required"
            passwordText.requestFocus()
            return false
        }

        // A password requires at least 6 characters
        if(password.length<6) {
            passwordText.error = "Minimum length of password should be 6"
            passwordText.requestFocus()
            return false
        }

        return true
    }

}
