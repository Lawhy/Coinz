package com.lawhy.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings


class SocialActivity : AppCompatActivity() {

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
    private var friendDocRef: DocumentReference? = null

    private val tag = "SocialActivity"
    // Collections storing information that will be displayed
    private val nameMap = HashMap<String, String>()
    private val goldMap = HashMap<String, Double>()
    private val rankedEmails = ArrayList<String>()

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

        friendDocRef = firestore?.collection("friends")?.document(userEmail)

        initView()
        initPersonalInfo()
    }

    override fun onStart() {
        super.onStart()
        setDataMaps()
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

            // Reject nickname that is too long or too short
            if(nickname.length > 15 || nickname.length < 3) {
                editNameView.visibility = View.GONE
                confirmNameBtn.visibility = View.GONE
                Toast.makeText(this, "Please select nickname of length 3-15 characters.", Toast.LENGTH_SHORT).show()
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

        // Also store nicknames in a separate collection for displaying them in friend list
        firestore?.collection("nicknames")
                ?.document(userEmail)
                ?.set(mapOf("name" to nickname))
                ?.addOnSuccessListener { Log.i(tag, "Successfully change the nickname!") }
                ?.addOnFailureListener { Log.wtf(tag, it.message) }

    }

    /* To display the friend list with (rank, name, gold),
    * it needs to load the necessary data from the database.
    * The strategy here is:
    * 1. Retrieve a list of friends' emails
    * 2. For each friend's email:
    *    2.1 update the nameMap (storing nicknames of friends)
    *    2.2 after nameMap is updated, update the goldMap (storing gold numbers of friends)
    * 3. After last gold number is added to goldMap, update the rankedEmails (storing friends' emails according to ranking of wealth)
    * 4. Display the friend list accordingly.
    * The four functions below are strongly related to each other.
    * */

    private fun setDataMaps() {

        friendDocRef?.get()?.addOnSuccessListener {
            val friends = it.data?.values?.toList()
            if (friends.isNullOrEmpty()){ Log.i(tag, "No friend at all.") }
            else {
                //Count the user him/herself as a friend
                updateNameMap(userEmail)
                updateGoldMap(userEmail)

                for (i in 0 until friends.size) {
                    val email = friends[i]
                    Log.d(tag, "A friend $email")
                    if(i == friends.size - 1) {
                        updateNameMap(email.toString(), isLast = true)
                    } else {
                        updateNameMap(email.toString()) // nameMap will trigger update of goldMap
                    }
                }
                Log.i(tag, "Data maps are constructed.")
            }
        }

    }

    private fun updateNameMap(email: String, isLast: Boolean=false) {

        firestore?.collection("nicknames")
                ?.document(email)
                ?.get()
                ?.addOnSuccessListener { n ->
                    val nickname = n.data
                    if(nickname.isNullOrEmpty()) {Log.i(tag, "No name at all.")}
                    else {
                        nameMap[email] = nickname["name"].toString()
                        Log.d(tag, "Check NAME MAP: $nameMap")
                        updateGoldMap(email, isLast) // goldMap will trigger update of rankList
                    }
                }?.addOnFailureListener { Log.wtf(tag, it.message) }

    }

    private fun updateGoldMap(email: String, isLast: Boolean=false) {

        firestore?.collection("gold")
                ?.document(email)
                ?.get()
                ?.addOnSuccessListener {
                    val data = it.data
                    if(data.isNullOrEmpty()) {Log.i(tag, "No Gold info at all.")}
                    else {
                        if(data["goldNumber"] != null) {
                            goldMap[email] = data["goldNumber"].toString().toDouble()
                        } else {
                            goldMap[email] = 0.000
                        }
                        if (isLast) {
                            // Somehow the last assignment takes a while, so I have to add this line
                            // waiting for full update of data maps.
                            while(nameMap.size != goldMap.size) {
                                Log.d(tag, "Waiting.")
                            }
                            updateRankedEmails()
                        }
                        Log.d(tag, "Check GOLD MAP: $goldMap")
                    }
                }?.addOnFailureListener { Log.wtf(tag, it.message) }
    }

    private fun updateRankedEmails() {
        rankedEmails.clear()
        Log.d(tag, "[Name!!!] $nameMap")
        Log.d(tag, "[Gold!!!] $goldMap")
        rankedEmails.addAll(goldMap.toList().sortedBy { (_, value) -> value}.toMap().keys.toList().reversed())
        Log.d(tag, "[Ranking!!!]: $rankedEmails")
        displayFriendsByRank()
    }

    private fun displayFriendsByRank() {

        userRankView.text = "Rank: ${rankedEmails.indexOf(userEmail) + 1}" // Set the user's rank.

        // Set the friend list
        friendListLayout.removeAllViews()
        for (i in 0 until rankedEmails.size) {

            // Get relevant data
            val email = rankedEmails[i]
            Log.d(tag, "[Rank $i] $email")
            val gold = goldMap[email]
            val nickname = nameMap[email]

            // Construct view for each friend
            val tr = TableRow(this)
            tr.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)

            // Rank Displayed
            val rankText = TextView(this)
            rankText.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1F)
            rankText.gravity = Gravity.CENTER
            rankText.text = (i+1).toString()

            // Name Displayed
            val nameText = TextView(this)
            nameText.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 3F)
            nameText.gravity = Gravity.CENTER
            nameText.text = nickname

            // Gold Displayed
            val goldText = TextView(this)
            goldText.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 3F)
            goldText.gravity = Gravity.CENTER
            goldText.text = "%.3f".format(gold)

            tr.addView(rankText)
            tr.addView(nameText)
            tr.addView(goldText)
            friendListLayout.addView(tr)
        }

    }


}
