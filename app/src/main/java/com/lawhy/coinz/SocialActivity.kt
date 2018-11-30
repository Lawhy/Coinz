package com.lawhy.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings


class SocialActivity : AppCompatActivity() {

    /** In this activity, data mostly saved in the pool collection which has different structure
    * with other collections. The point is to gather small data together such as (3 documents)
    *  1. customized nick name
    *  2. download date
    *  3. user email
    * Here each document uses UserID as a key, whereas in other collections, the userEmails are used
    * for document names and descriptive strings are used for keys.
    * The pool collection looks like:
    *     pool/names/{userID -> nickname}
    * whereas others look like:
    *     maps/userEmail/{"mapToday" -> Coinz Map}
    * */

    private lateinit var userNickNameView: TextView
    private lateinit var userEmailView: TextView
    private lateinit var userRankView: TextView
    private lateinit var friendListLayout: TableLayout
    private lateinit var editNameBtn: Button
    private lateinit var editNameView: EditText
    private lateinit var confirmNameBtn: Button
    private lateinit var userEmail: String

    // Firebase
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private lateinit var userID: String
    private var firestore: FirebaseFirestore? = null

    private val tag = "SocialActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_social)

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

        initView()
        initPersonalInfo()
    }

    override fun onStart() {
        super.onStart()
    }


    private fun initView() {
        userNickNameView = findViewById(R.id.userNickName)
        userEmailView = findViewById(R.id.userEmail)
        userRankView = findViewById(R.id.userRank)
        friendListLayout = findViewById(R.id.friendList)
        editNameView = findViewById(R.id.editNickName)
        editNameView.visibility = View.GONE
        editNameBtn = findViewById(R.id.nameBtn)
        confirmNameBtn = findViewById(R.id.confirmNameBtn)
        confirmNameBtn.visibility = View.GONE

        editNameBtn.setOnClickListener {
            editNameView.visibility = View.VISIBLE
            confirmNameBtn.visibility = View.VISIBLE
            editNameView.requestFocus()
        }

        confirmNameBtn.setOnClickListener {

            val nickname: String = editNameView.text.toString()
            val curname: String = userNickNameView.text.toString()

            // That is no change at all.
            if(nickname == curname) {
                editNameView.visibility = View.GONE
                confirmNameBtn.visibility = View.GONE
                return@setOnClickListener
            }

            val registeredNamesDoc = firestore?.collection("pool")?.document("names")
            val namesInPool = ArrayList<String>()
            registeredNamesDoc?.get()
                    ?.addOnSuccessListener { snapshot ->
                        val data = snapshot.data
                        if (data.isNullOrEmpty()) {
                            Log.i(tag, "No names in the data pool yet.")
                            registeredNamesDoc.set(mapOf())
                        } else {
                            for (k in data.keys) {
                                namesInPool.add(data[k].toString())
                            }
                            Log.i(tag, "${namesInPool.size} names in the pool have been retrieved.")
                        }
                        if (namesInPool.contains(nickname)) {
                            Log.i(tag, "Name has been used.")
                            Toast.makeText(this, "Name has been occupied, please use another one.", Toast.LENGTH_SHORT).show()
                        } else {
                            // Change the nick name only when it is unique
                            changeNickName(nickname)
                            editNameView.visibility = View.GONE
                            confirmNameBtn.visibility = View.GONE
                            userNickNameView.text = nickname
                        }
                    }
        }

    }

    private fun initPersonalInfo() {

        Log.i(tag, "Current user's email is: $userEmail")
        userEmailView.text = "($userEmail)"
        firestore?.collection("pool")
                ?.document("names")
                ?.get()
                ?.addOnSuccessListener {
                    val data  = it.data
                    if (data.isNullOrEmpty()) {
                        Log.i(tag, "No name in the pool.")
                        userNickNameView.text = userEmail // Default name is the email
                    } else {
                        val nickname = data[userID].toString()
                        if (nickname.isNotEmpty()) {userNickNameView.text = nickname}
                        else {userNickNameView.text = userEmail} // Default name is the email
                        Log.i(tag, "$userEmail nick name is $nickname")
                    }
                }
                ?.addOnFailureListener { Log.wtf(tag, "Fail to load user names!") }

        // store the user emails to the pool collection as well
        firestore?.collection("pool")
                ?.document("emails")
                ?.get()
                ?.addOnSuccessListener {
                    val data = it.data
                    if (data.isNullOrEmpty()) {
                        firestore?.collection("pool")
                                ?.document("emails")
                                ?.set(mapOf(userID to userEmail))
                    } else {
                        firestore?.collection("pool")
                                ?.document("emails")
                                ?.update(mapOf(userID to userEmail))
                    }
                }
    }

    private fun changeNickName(nickname: String) {

        firestore?.collection("pool")
                ?.document("names")
                ?.update(mapOf(userID to nickname))
                ?.addOnSuccessListener {
                    Log.i(tag, "Successfully change the nickname!")
                    Toast.makeText(this, "Successfully save nickname.", Toast.LENGTH_SHORT).show()

                }
                ?.addOnFailureListener {
                    Log.wtf(tag, "Not able to change the name.")
                    Toast.makeText(this, "Fail to save nickname.", Toast.LENGTH_SHORT).show()
                }

    }
}
