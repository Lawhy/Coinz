package com.lawhy.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import com.google.firebase.auth.FirebaseAuthUserCollisionException


class SignUpActivity : AppCompatActivity() {

    /** This activity uses fire-base authentication to provide user sign-up
     *  Some rules of account and password are set.
     * */

    private lateinit var emailText: EditText
    private lateinit var passwordText: EditText
    private lateinit var confirmText: EditText

    private var mAuth: FirebaseAuth? = null
    private val tag: String = "SignUpActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialise everything
        emailText = findViewById(R.id.suEmail)
        passwordText = findViewById(R.id.suPassword)
        confirmText = findViewById(R.id.suConfirmed)
        mAuth = FirebaseAuth.getInstance()
        mySetOnClick()

    }

    private fun mySetOnClick() {

        // SignUpBtn
        val signUpBtn = findViewById<Button>(R.id.suSignUpBtn)
        signUpBtn.setOnClickListener { registerUser() }

        // SignInBtn
        val signInBtn = findViewById<Button>(R.id.suSignInBtn)
        signInBtn.setOnClickListener { startActivity(Intent(this, AuthenticationActivity::class.java)) }

        // CloseBtn
        val closeBtn = findViewById<ImageButton>(R.id.closeSuBtn)
        closeBtn.setOnClickListener { startActivity(Intent(this, MainActivity::class.java))  }
    }

    private fun registerUser() {
        val email = emailText.text.toString().trim()
        val password = passwordText.text.toString().trim()
        val confirm = confirmText.text.toString().trim()

        if (!checkValidSignUp(email, password, confirm)) return // Return if not valid

        // Use the firebase method to create an user
        mAuth?.createUserWithEmailAndPassword(email, password)
                ?.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(tag, "createUserWithEmail:success")
                        Toast.makeText(this, "Successfully create an account!",
                                Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, AuthenticationActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(tag, "createUserWithEmail:failure", task.exception)
                        if(task.exception is FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "The account has already existed!",
                                    Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, task.exception?.message,
                                    Toast.LENGTH_SHORT).show()
                        }
                    }
                }

    }

    private fun checkValidSignUp(email:String, password:String, confirm:String):Boolean {
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

        if(confirm.isEmpty()) {
            confirmText.error = "Please re-enter your password"
            confirmText.requestFocus()
            return false
        }

        // Need to confirm the password correctly
        if(confirm != password) {
            confirmText.error = "Check any typo! The passwords do not match"
            confirmText.requestFocus()
            return false
        }

        return true
    }


}
