package com.lawhy.coinz

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Gravity
import android.view.Gravity.CENTER
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class RequestActivity : AppCompatActivity() {

    /** The requests collection has document for each userEmail
     * In each document, there exists two keys {friend, coin}
     * data["friend"] = List of friend requests specified by emails
     * data["coin"] = List of foreign coins sent to the userEmail
     * */

    private lateinit var searchFriendView: EditText
    private lateinit var requestFriendBtn: Button
    private lateinit var friendReqsLayout: TableLayout

    // Firebase
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private lateinit var userID: String
    private lateinit var userEmail: String
    private var firestore: FirebaseFirestore? = null
    private var requestDocRef: DocumentReference? = null
    private var emailDocRef: DocumentReference? = null
    private var friendDocRef: DocumentReference? = null

    private val tag = "RequestActivity"
    private val emailsFromRequests = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request)

        searchFriendView = findViewById(R.id.editAddFriend)
        requestFriendBtn = findViewById(R.id.requestFriendBtn)
        friendReqsLayout = findViewById(R.id.friendReqs)

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
        requestDocRef = firestore?.collection("requests")?.document(userEmail)
        emailDocRef = firestore?.collection("pool")?.document("emails")
        friendDocRef = firestore?.collection("friends")?.document(userEmail)

        setRequestBtn()
    }

    override fun onStart() {
        super.onStart()
        displayFriendRequests()
    }



    /* Send friend request strategy:
    * 1. check marginal case: user adds him/her-self as friend.
    * 2. check if searched email exists in data pool, stop if not.
    *    (this will be true if the email is used for registration)
    * 3. If so, check if the searched email is already in the friend list, stop if not.
    * 4. If so, send the friend request to the requests collection, waiting for response.
    * */


    private fun setRequestBtn() {

        requestFriendBtn.setOnClickListener {
            val friendEmail: String = searchFriendView.text.toString()

            // Marginal case: user adds himself/herself as friend
            if(friendEmail == userEmail) {
                Log.i(tag, "Cannot add yourself as a friend.")
                Toast.makeText(this, "Cannot add yourself as a friend!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if the email exists in database
            emailDocRef?.get()?.addOnSuccessListener { snapShotEmails ->
                        val data = snapShotEmails.data
                        if(data.isNullOrEmpty()) {
                            Log.wtf(tag, "No email reference in database!")
                        } else {
                            for (id in data.keys) {
                                val em = data[id]
                                if (em == friendEmail) {
                                    checkIsFriend(friendEmail)
                                    Log.d(tag, "Found email in database.")
                                    return@addOnSuccessListener
                                }
                            }
                            // Inform user that searched account doesn't exist and clear the input text
                            Toast.makeText(this, "Searched account does not exist!", Toast.LENGTH_SHORT).show()
                            searchFriendView.text.clear()
                        }
            }?.addOnFailureListener { e -> Log.wtf(tag, e.message) }

        }

    }

    private fun checkIsFriend(friendEmail: String) {

        friendDocRef?.get()?.addOnSuccessListener {
            var notFriend = true
            val friendList = it.data
            if (friendList.isNullOrEmpty()) {friendDocRef?.set(mapOf())}
            else {
                for (em in friendList.values) {
                    if (friendEmail == em) {
                        notFriend = false
                        break
                    }
                }
            }
            // different results for is/not a friend
            if (notFriend) {
                sendFriendRequest(friendEmail)
            }
            else {
                Log.i(tag, "Friend already exists.")
                Toast.makeText(this, "Friend already exists.", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun sendFriendRequest(requestedEmail: String) {

        val friendRequestDoc = firestore?.collection("requests")?.document(requestedEmail)
        friendRequestDoc?.get()?.addOnSuccessListener {
            val requests = it.data
            if (requests.isNullOrEmpty()) {
                Log.d(tag, "Requests storage has not been established.")
                friendRequestDoc.set(mapOf("0" to userEmail)).addOnSuccessListener {
                    Log.d(tag, "First request in store.")
                    Toast.makeText(this, "Friend request sent successfully!", Toast.LENGTH_SHORT).show()
                }
            } else {
                val index:Int = requests.size
                Log.d(tag, "Update friend request.")
                friendRequestDoc.update(mapOf(index.toString() to userEmail))
                        .addOnSuccessListener {
                            Log.i(tag, "Friend request sent successfully $userEmail -> $requestedEmail")
                            Toast.makeText(this, "Friend request sent successfully!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { Log.wtf(tag, "Cannot send a request!") }
            }
        }

    }

    private fun displayFriendRequests() {

        emailsFromRequests.clear()

        requestDocRef?.get()?.addOnSuccessListener {
            val reqData = it.data
            if (reqData.isNullOrEmpty()) {
                Log.d(tag, "Friend requests have not been established in database.")
                requestDocRef?.set(mapOf())?.addOnSuccessListener { Log.d(tag, "Friend requests established in database.") }
            } else {
                for (k in reqData.keys) {
                    val request = reqData[k].toString()
                    emailsFromRequests.add(request)
                }
                updateFriendRequestsView(emailsFromRequests)
            }
        } ?.addOnFailureListener { Log.wtf(tag, it.message) }

    }

    private fun updateFriendRequestsView(emails: ArrayList<String>) {

        friendReqsLayout.removeAllViews()

        for (i in 0 until emails.size) {
            val tr = TableRow(this)
            tr.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)

            // Display index
            val indexText = TextView(this)
            indexText.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1F)
            indexText.text = i.toString()

            // Display Email
            val emailText = TextView(this)
            emailText.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 3F)
            emailText.gravity = CENTER
            emailText.text = emails[i]

            // Accept Button
            val acceptBtn = Button(this)
            acceptBtn.setBackgroundColor(Color.TRANSPARENT)
            acceptBtn.setTextColor(Color.parseColor("#ff5d5e"))
            acceptBtn.text = "Accept"
            acceptBtn.textSize = 12f
            acceptBtn.gravity = Gravity.CENTER

            // Decline Button
            val declineBtn = Button(this)
            declineBtn.setBackgroundColor(Color.TRANSPARENT)
            declineBtn.setTextColor(Color.parseColor("#4264fb"))
            declineBtn.text = "Decline"
            declineBtn.textSize = 12f
            declineBtn.gravity = Gravity.CENTER

            tr.addView(indexText)
            tr.addView(emailText)
            tr.addView(acceptBtn)
            tr.addView(declineBtn)
            friendReqsLayout.addView(tr)

            // Set button listener
            acceptBtn.setOnClickListener {
                val em = emails[i]
                val alertDialog = AlertDialog.Builder(this)
                alertDialog.setTitle("Are you sure?")
                alertDialog.setMessage("Receive $em as a friend.")
                alertDialog.setPositiveButton("YES") { _, _ ->

                    // Friend bridge user -> request sender
                    friendDocRef?.get()?.addOnSuccessListener { snap ->
                        val friendList = snap.data
                        if (friendList.isNullOrEmpty()) {
                            friendDocRef?.set(mapOf("0" to em))
                                    ?.addOnSuccessListener { Log.i(tag, "$userEmail adds $em into friend list.") }
                                    ?.addOnFailureListener { e -> Log.wtf(tag, e.message) }
                        } else {
                            val new = friendList.size.toString()
                            friendDocRef?.update(mapOf(new to em))
                                    ?.addOnSuccessListener { Log.i(tag, "$userEmail adds $em into friend list.") }
                                    ?.addOnFailureListener { e -> Log.wtf(tag, e.message) }
                        }
                    }

                    // Friend bridge request sender -> user
                    // user should be added to the friend list of whom the request are from
                    val friendReqDocRef = firestore?.collection("friends")?.document(em)
                    friendReqDocRef?.get()?.addOnSuccessListener { snapReq ->
                        val friendListFromRequest = snapReq.data
                        if (friendListFromRequest.isNullOrEmpty()) {
                            friendReqDocRef.set(mapOf("0" to userEmail))
                        } else {
                            val new1 = friendListFromRequest.size.toString()
                            friendReqDocRef.update(mapOf(new1 to userEmail))
                            Log.i(tag, "$em adds $userEmail into friend list.")
                        }
                    }

                    Toast.makeText(this, "Successfully add a friend!", Toast.LENGTH_SHORT).show()
                    emails.remove(em)
                    updateFriendRequests(emails)
                    updateFriendRequestsView(emails)
                }

                alertDialog.setNegativeButton("NO") { _, _ -> }
                alertDialog.show()
            }

            declineBtn.setOnClickListener {
                val em = emails[i]
                val alertDialog = AlertDialog.Builder(this)
                alertDialog.setTitle("Are you sure?")
                alertDialog.setMessage("Decline the friend request from $em.")
                alertDialog.setPositiveButton("YES"){ _,_ ->
                    emails.remove(em)
                    updateFriendRequests(emails)
                }
                alertDialog.setNegativeButton("NO") {_,_ ->}
                alertDialog.show()
            }
        }
    }


    private fun updateFriendRequests(emails: ArrayList<String>) {
        requestDocRef?.set(mapOf())?.addOnSuccessListener {
            Log.d(tag, "Clean all the friend requests before update.")
            for (i in 0 until emails.size) {
                requestDocRef?.update(mapOf(i.toString() to emails[i]))
                        ?.addOnSuccessListener {
                            Log.i(tag, "${i}th request has been renewed")
                        }
                        ?.addOnFailureListener { e -> Log.wtf(tag, e.message) }
            }
        }
    }
}





