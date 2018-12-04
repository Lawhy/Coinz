package com.lawhy.coinz

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.time.LocalDateTime

class MyAccountActivity : AppCompatActivity() {

    /** This activity deals with the Personal Account of the user.
     * Main functionality:
     *   1. Display and update statistics.
     *   2. Set up local wallet (simply for more efficient statistics display)
     *   2. Bank local/foreign coins.
     *   3. Send local coin (foreign coin not allowed to send, prevent unlimited transaction)
     * The statistics summary:
     *   1. Number of Gold
     *   2. For each type of the currency {SHIL, DOLR, QUID, PENY}:
     *      a. Number of local coins
     *      b. Number of foreign coins (Received from other players)
     *      c. Value (In total)
     *      d. Exchange-Rate (Daily X-Rate)
     * Data storage used here:
     *   1. friends -> userEmail -> list of friends' emails
     *   2. gold -> userEmail -> goldNumber, bankedNumber
     *   3. coins -> userEmail -> list of local coins
     *   4. foreignCoins -> userEmail -> list of foreign coins
     * Also, send button will trigger a dialog showing a list of friend (i.e. you can only send coin to a friend)
     * */

    private lateinit var exchangeRates: HashMap<*, *>
    private lateinit var wallet: Wallet // local wallet
    private var goldNumber: Double = 0.000
    private var bankedNum = 0 // banked number of coins today (FOREIGN COINS ARE NOT LIMITED BY THIS)
    private val bankLIMIT = 25 // limit of banking number for collected coins
    private var mapDate = MyUtils().getCurrentDate() // to check if the bankedNum should be renewed


    // Statistics components
    private lateinit var goldNumberView: TextView
    // SHIL
    private lateinit var localSHIL: TextView
    private lateinit var foreignSHIL: TextView
    private lateinit var valSHIL: TextView
    private lateinit var xrSHIL: TextView
    // DOLR
    private lateinit var localDOLR: TextView
    private lateinit var foreignDOLR: TextView
    private lateinit var valDOLR: TextView
    private lateinit var xrDOLR: TextView
    // QUID
    private lateinit var localQUID: TextView
    private lateinit var foreignQUID: TextView
    private lateinit var valQUID: TextView
    private lateinit var xrQUID: TextView
    // PENY
    private lateinit var localPENY: TextView
    private lateinit var foreignPENY: TextView
    private lateinit var valPENY: TextView
    private lateinit var xrPENY: TextView

    // Display day of the week to remind players to spend coins before Monday
    private lateinit var dayOfWeekView: TextView

    // Local/Foreign coins list views
    private lateinit var coinsListView: TableLayout
    private lateinit var foreignListView: TableLayout

    // Display banked number of coins today (FOREIGN COINS ARE NOT LIMITED BY THIS)
    private lateinit var bankNumberView: TextView

    // Fire-base
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private lateinit var userID: String
    private lateinit var userEmail: String
    private var firestore: FirebaseFirestore? = null
    private var friendDocRef: DocumentReference? = null
    private var goldDocRef: DocumentReference? = null
    private var coinsDocRef: DocumentReference? = null
    private var foreignCoinsDocRef: DocumentReference? = null

    private val tag = "MyAccountActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_account)

        exchangeRates = intent.extras?.get("exchangeRates") as HashMap<*, *>

        // Fire-base Initialization
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
        goldDocRef = firestore?.collection("gold")?.document(userEmail)
        coinsDocRef = firestore?.collection("coins")?.document(userEmail)
        foreignCoinsDocRef = firestore?.collection("foreignCoins")?.document(userEmail)

        initStatsViews()
        initGoldStat()
    }

    override fun onStart() {
        super.onStart()
        setBankedNumber()
        localWallet()
    }

    override fun onStop() {
        super.onStop()
        updateBankedNumber()
        updateGoldStat()
    }

    private fun initStatsViews() {
        // GOLD
        goldNumberView = findViewById(R.id.goldNumber)

        // SHIL
        localSHIL = findViewById(R.id.localSHIL)
        foreignSHIL = findViewById(R.id.foreignSHIL)
        valSHIL = findViewById(R.id.valSHIL)
        xrSHIL = findViewById(R.id.xrSHIL)

        // DOLR
        localDOLR = findViewById(R.id.localDOLR)
        foreignDOLR = findViewById(R.id.foreignDOLR)
        valDOLR = findViewById(R.id.valDOLR)
        xrDOLR = findViewById(R.id.xrDOLR)

        // QUID
        localQUID = findViewById(R.id.localQUID)
        foreignQUID = findViewById(R.id.foreignQUID)
        valQUID = findViewById(R.id.valQUID)
        xrQUID = findViewById(R.id.xrQUID)

        // PENY
        localPENY = findViewById(R.id.localPENY)
        foreignPENY = findViewById(R.id.foreignPENY)
        valPENY = findViewById(R.id.valPENY)
        xrPENY = findViewById(R.id.xrPENY)

        dayOfWeekView = findViewById(R.id.dayOfWeek)
        val dayText = "${LocalDateTime.now().dayOfWeek} now, expiration on next MONDAY."
        dayOfWeekView.text = dayText

        // local/foreign coins list
        coinsListView = findViewById(R.id.coinsList)
        foreignListView = findViewById(R.id.foreignCoinsList)

        // banked  coin number today
        bankNumberView = findViewById(R.id.BankNumber)

    }

    private fun setBankedNumber() {

        // In the gold collection, each userEmail is a document
        // and each document contains two fields: goldNumber, bankedNumber
        goldDocRef?.get()
                ?.addOnSuccessListener {
                    val data = it.data
                    if (data.isNullOrEmpty()) {
                        Log.i(tag, "No gold/bank number information.")
                        val originalMap = HashMap<String, Any>()
                        originalMap["goldNumber"] = 0.000
                        originalMap["bankedNumber"] = 0
                        goldDocRef?.set(originalMap)

                    } else {
                        if (mapDate != MyUtils().getCurrentDate()) {
                            Log.i(tag, "Renew banked number for a new day!")
                            mapDate = MyUtils().getCurrentDate()
                            bankedNum = 0
                            goldDocRef?.update(mapOf("bankedNumber" to 0))
                        } else {
                            bankedNum = if (data.keys.contains("bankedNumber")) {
                                data["bankedNumber"].toString().toInt()
                            } else {
                                0
                            }
                            Log.i(tag, "The banked coin number is $bankedNum")
                        }
                    }
                    // Update the banked number view accordingly
                    bankNumberView.text = "You have banked $bankedNum coins today (Limit: 25)."
                }
    }

    private fun updateBankedNumber() {

        bankNumberView.text = "You have banked $bankedNum coins today (Limit: 25)."

        goldDocRef?.update(mapOf("bankedNumber" to bankedNum))
                ?.addOnSuccessListener { Log.i(tag, "Banked Coin Number has been uploaded to Database.") }
                ?.addOnFailureListener { Log.wtf(tag, "Banked Coin Number cannot be uploaded!") }

    }

    private fun initGoldStat() {

        // Retrieve stored Gold Number from Fire-store
        goldDocRef?.get()
                ?.addOnSuccessListener {
                    val data = it.data
                    if (data.isNullOrEmpty()) {
                        Log.i(tag, "No Gold Info in the bank account.")
                        val originalMap = HashMap<String, Any>()
                        originalMap["goldNumber"] = 0.000
                        originalMap["bankedNumber"] = 0
                        goldDocRef?.set(originalMap)
                                ?.addOnSuccessListener { Log.i(tag, "Init Gold Number in account.") }
                                ?.addOnFailureListener { Log.wtf(tag, "Gold Number cannot be initialised!") }
                    } else {
                        goldNumber = data["goldNumber"].toString().toDouble()
                        goldNumberView.text = "%.3f".format(goldNumber)
                        Log.i(tag, "Account has Gold %.3f".format(goldNumber))
                    }
                }
    }

    private fun updateGoldStat(increase: Double = 0.000) {

        // Increase the goldNumber
        goldNumber += increase

        // Upload gold number to fire-store
        goldDocRef?.update(mapOf("goldNumber" to goldNumber))
                ?.addOnSuccessListener { Log.i(tag, "Gold Number has been updated!") }
                ?.addOnFailureListener { Log.wtf(tag, "Gold Number cannot be updated!") }

        // Display gold number in 3 s.f.
        goldNumberView.text = "%.3f".format(goldNumber)

    }

    // Retrieve both the coins and the foreign coins from online storage, fill them into the local wallet.
    private fun localWallet() {

        wallet = Wallet(ArrayList(), ArrayList())

        // Retrieve stored coins information from Fire-store
        coinsDocRef?.get()
                ?.addOnSuccessListener {
                    val data = it.data
                    if (data.isNullOrEmpty()) {
                        Log.i(tag, "No coins in the wallet.")
                    } else {
                        for (coinMap in data) {
                            Log.i("[userEmail:$userEmail]", "Coins are filled in the local coins.")
                            val index = coinMap.key as String
                            val properties = coinMap.value as HashMap<*, *>
                            val id = properties["id"] as String
                            val currency = properties["currency"] as String
                            val value = properties["value"] as Double
                            val coin = Coin(id, currency, value, null) // Here marker is not important
                            Log.i("[coin$index]", "$currency: $value")
                            wallet.add(coin) // These coins are all local coins
                        }

                        // Fill foreign coins into the wallet as well
                        foreignCoinsDocRef?.get()?.addOnSuccessListener { f ->
                            val foreignData = f.data
                            if (foreignData.isNullOrEmpty()) {
                                Log.i(tag, "No foreign coins in the wallet.")
                            } else {
                                for (foreignCoinMap in foreignData) {
                                    Log.i("[userEmail:$userEmail]", "Foreign coins are filled in the local wallet.")
                                    val fIndex = foreignCoinMap.key as String
                                    val fProperties = foreignCoinMap.value as HashMap<*, *>
                                    val fId = fProperties["id"] as String
                                    val fCurrency = fProperties["currency"] as String
                                    val fValue = fProperties["value"] as Double
                                    val foreignCoin = Coin(fId, fCurrency, fValue, null) // Here marker is not important
                                    Log.i("[ForeignCoin$fIndex]", "$fCurrency: $fValue")
                                    wallet.addForeign(foreignCoin) // Add the foreign coins into the wallet
                                }
                            }
                            // Update everything when wallet is prepared
                            updateXRates()
                            updateValue(wallet)
                            updateLocal(wallet)
                            updateForeign(wallet)
                            updateCoinsListView(wallet)
                            updateForeignCoinsListView(wallet)
                        }
                    }
                }
    }

    private fun updateXRates() {

        for (k in exchangeRates.keys) {
            val currency = k.toString()
            when (currency) {
                "SHIL" -> xrSHIL.text = "%.3f".format(exchangeRates[k] as Double)
                "DOLR" -> xrDOLR.text = "%.3f".format(exchangeRates[k] as Double)
                "QUID" -> xrQUID.text = "%.3f".format(exchangeRates[k] as Double)
                "PENY" -> xrPENY.text = "%.3f".format(exchangeRates[k] as Double)
                else -> Log.d(tag, "No Exchange Rates!")
            }
        }
    }

    private fun updateValue(wallet: Wallet) {

        var valS = 0.000
        var valD = 0.000
        var valQ = 0.000
        var valP = 0.000

        // Attach local coin values
        for (coin in wallet.coins) {
            val currency = coin.currency
            when (currency) {
                "SHIL" -> {
                    valS += coin.value
                }
                "DOLR" -> valD += coin.value
                "QUID" -> valQ += coin.value
                "PENY" -> valP += coin.value
                else -> Log.d(tag, "No coins in the wallet!")
            }
        }

        // Attach foreign coin values
        for (coin in wallet.foreignCoins) {
            val currency = coin.currency
            when (currency) {
                "SHIL" -> {
                    valS += coin.value
                }
                "DOLR" -> valD += coin.value
                "QUID" -> valQ += coin.value
                "PENY" -> valP += coin.value
                else -> Log.d(tag, "No coins in the wallet!")
            }
        }

        valSHIL.text = "%.3f".format(valS)
        valDOLR.text = "%.3f".format(valD)
        valQUID.text = "%.3f".format(valQ)
        valPENY.text = "%.3f".format(valP)
    }

    private fun updateLocal(wallet: Wallet) {

        var localS = 0
        var localD = 0
        var localQ = 0
        var localP = 0

        for (coin in wallet.coins) {
            val currency = coin.currency
            when (currency) {
                "SHIL" -> localS += 1
                "DOLR" -> localD += 1
                "QUID" -> localQ += 1
                "PENY" -> localP += 1
                else -> Log.d(tag, "No coins in the wallet!")
            }
        }

        localSHIL.text = localS.toString()
        localDOLR.text = localD.toString()
        localQUID.text = localQ.toString()
        localPENY.text = localP.toString()

    }

    private fun updateForeign(wallet: Wallet) {

        var foreignS = 0
        var foreignD = 0
        var foreignQ = 0
        var foreignP = 0

        for (coin in wallet.foreignCoins) {
            val currency = coin.currency
            when (currency) {
                "SHIL" -> foreignS += 1
                "DOLR" -> foreignD += 1
                "QUID" -> foreignQ += 1
                "PENY" -> foreignP += 1
                else -> Log.d(tag, "No coins in the wallet!")
            }
        }

        foreignSHIL.text = foreignS.toString()
        foreignDOLR.text = foreignD.toString()
        foreignQUID.text = foreignQ.toString()
        foreignPENY.text = foreignP.toString()

    }

    private fun updateCoinsListView(wallet: Wallet) {

        coinsListView.removeAllViews()

        for (i in 0 until wallet.coins.size) {

            val coin = wallet.coins[i]

            val tr = TableRow(this)
            tr.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)

            // Display coinID
            val tvID = TextView(this)
            tvID.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1F)
            tvID.text = i.toString()
            tvID.gravity = Gravity.CENTER

            // Display currency
            val tvCurrency = TextView(this)
            tvCurrency.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1F)
            tvCurrency.text = coin.currency
            tvCurrency.gravity = Gravity.CENTER

            // Display value
            val tvValue = TextView(this)
            tvValue.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 3F)
            tvValue.text = "%.3f".format(coin.value)
            tvValue.gravity = Gravity.CENTER

            // Send Button
            val sendBtn = Button(this)
            sendBtn.setBackgroundColor(Color.TRANSPARENT)
            sendBtn.setTextColor(Color.parseColor("#ff5d5e"))
            sendBtn.text = "Send"
            sendBtn.textSize = 12f
            sendBtn.gravity = Gravity.CENTER


            // Bank Button
            val bankBtn = Button(this)
            bankBtn.setBackgroundColor(Color.TRANSPARENT)
            bankBtn.setTextColor(Color.parseColor("#4264fb"))
            bankBtn.text = "Bank"
            bankBtn.textSize = 12f
            bankBtn.gravity = Gravity.CENTER

            tr.addView(tvID)
            tr.addView(tvCurrency)
            tr.addView(tvValue)
            tr.addView(sendBtn)
            tr.addView(bankBtn)
            coinsListView.addView(tr)

            setBankBtnOnClick(bankBtn, coin)
            setSendBtnOnClick(sendBtn, coin)
        }
    }

    private fun updateForeignCoinsListView(wallet: Wallet) {

        foreignListView.removeAllViews()

        for (i in 0 until wallet.foreignCoins.size) {

            val foreignCoin = wallet.foreignCoins[i]

            val tr = TableRow(this)
            tr.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)

            // Display coinID
            val tvID = TextView(this)
            tvID.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1F)
            tvID.text = i.toString()
            tvID.gravity = Gravity.CENTER

            // Display currency
            val tvCurrency = TextView(this)
            tvCurrency.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1F)
            tvCurrency.text = foreignCoin.currency
            tvCurrency.gravity = Gravity.CENTER

            // Display value
            val tvValue = TextView(this)
            tvValue.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 3F)
            tvValue.text = "%.3f".format(foreignCoin.value)
            tvValue.gravity = Gravity.CENTER

            // Display cannot send message: BOUND
            val tvBound = TextView(this)
            tvBound.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1F)
            tvBound.text = "BOUND"
            tvBound.gravity = Gravity.CENTER

            // Bank Button, No send button
            val bankBtn = Button(this)
            bankBtn.setBackgroundColor(Color.TRANSPARENT)
            bankBtn.setTextColor(Color.parseColor("#4264fb"))
            bankBtn.text = "Bank"
            bankBtn.textSize = 12f
            bankBtn.gravity = Gravity.CENTER

            tr.addView(tvID)
            tr.addView(tvCurrency)
            tr.addView(tvValue)
            tr.addView(tvBound) // Important to notice that foreign coin is BOUND, i.e. cannot be sent
            tr.addView(bankBtn)
            foreignListView.addView(tr)

            setBankBtnOnClick(bankBtn, foreignCoin, isForeign = true)
        }

    }

    private fun setBankBtnOnClick(bankBtn: Button, coin: Coin, isForeign: Boolean = false) {

        bankBtn.setOnClickListener {
            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle("Sure to bank this coin?")
            alertDialog.setMessage("${coin.currency}: %.3f".format(coin.value))
            alertDialog.setPositiveButton("YES") { _, _ ->
                val gain = if (isForeign) {
                    bankForeignCoin(coin)
                } else {
                    bankCoin(coin)
                }
                if (gain > 0) {
                    Log.i(tag, "Gained Gold $gain")
                    updateGoldStat(increase = gain)
                } else {
                    Log.i(tag, "Coin is not allowed to banked.")
                }
            }
            alertDialog.setNegativeButton("NO") { _, _ -> }
            alertDialog.show()
        }
    }

    private fun setSendBtnOnClick(sendBtn: Button, coin: Coin) {

        sendBtn.setOnClickListener {

            val alertDialog = AlertDialog.Builder(this@MyAccountActivity)
            val view = View.inflate(this, R.layout.send_coin_dialog, null)
            val friendsLayout: TableLayout = view.findViewById(R.id.sendCoinFriendList) // Display friend list on this layout
            var choiceToSend = "" // For one send request, only one choice of email.
            friendDocRef?.get()?.addOnSuccessListener { snapShotFriend ->
                val friends = snapShotFriend.data?.values
                if (friends.isNullOrEmpty()) {
                    Log.d(tag, "No friend at all.")
                } else {
                    Log.d(tag, "Retrieve the friend list.")
                    val textNameViews = ArrayList<TextView>() // Create this collection for animation of selection effect
                    // Add a table row for each friend's email
                    for (em in friends) {
                        val tr = TableRow(this)
                        tr.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
                        // A text view to display the email
                        val textName = TextView(this)
                        textName.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 2f)
                        textName.textSize = 18f
                        textName.setPadding(8,0,0,8)
                        textName.gravity = Gravity.CENTER
                        textName.text = em.toString()
                        textName.isClickable = true

                        // Set the on click listener for the textName to create a selection effect
                        textName.setOnClickListener {
                            textName.background = getDrawable(R.color.mapbox_blue) // Highlight the choice
                            choiceToSend = em.toString()
                            for (tv in textNameViews) {
                                if (tv != textName) {
                                    tv.background = getDrawable(R.color.transparent) // Unhighlight the rest
                                }
                            }
                        }

                        textNameViews.add(textName)
                        tr.addView(textName)
                        friendsLayout.addView(tr)
                    }

                }
            }?.addOnFailureListener { e -> Log.wtf(tag, e.message) }

            // Send the coin to the target friend
            alertDialog.setPositiveButton("Send") { _, _ ->
                if (choiceToSend == "") {
                    Toast.makeText(this, "Please select someone body!", Toast.LENGTH_SHORT).show()
                } else {
                    sendCoinTo(coin, choiceToSend)
                    Toast.makeText(this, "Coin sent to $choiceToSend", Toast.LENGTH_SHORT).show()
                }
            }
            alertDialog.setNegativeButton("Cancel") { _, _ -> }
            alertDialog.setView(view)
            alertDialog.show()
        }

    }

    // Notice that foreign coin cannot be sent again (to prevent players from making unlimited exchange)
    // This functionality will suppress the offline commercial activity (like making money by reselling the bought coins in a higher price)
    private fun sendCoinTo(coin: Coin, email: String) {

        // Add the sent coin to the foreignCoins collection according to set email.
        val foreignCoinsDocRef = firestore?.collection("foreignCoins")?.document(email)

        val coinMap = HashMap<String, Any>()
        coinMap["id"] = coin.id
        coinMap["currency"] = coin.currency
        coinMap["value"] = coin.value
        coinMap["from"] = userEmail

        foreignCoinsDocRef?.get()
                ?.addOnSuccessListener {
                    val data = it.data
                    if (data.isNullOrEmpty()) {
                        foreignCoinsDocRef.set(mapOf("0" to coinMap)) // Initialise if null or empty
                    } else {
                        val new = data.size.toString()
                        foreignCoinsDocRef.update(mapOf(new to coinMap))
                    }
                    // Can utilize the bankCoin method but not really update the gold number
                    // to delete the corresponding coin from wallet
                    bankedNum -= 1 // Since it is not real bank, bankedNum should be kept unchanged
                    bankCoin(coin)
                    updateCoinsListView(wallet)
                    updateLocal(wallet)
                    updateValue(wallet)
                }
                ?.addOnFailureListener { e -> Log.wtf(tag, e.message) }
    }

    private fun bankCoin(coinToBank: Coin): Double {

        // Bank only happens before exceeding the limit of 25 coins
        if (bankedNum <= bankLIMIT) {
            val xr = exchangeRates[coinToBank.currency].toString().toDouble()
            val value = coinToBank.value
            wallet.coins.remove(coinToBank)
            updateCoinsListView(wallet)
            // Replacing the online wallet by local wallet
            coinsDocRef
                    ?.set(mapOf()) // First remove everything
                    ?.addOnSuccessListener {
                        for (i in 0 until wallet.coins.size) {
                            val coin = wallet.coins[i]
                            val coinMap = HashMap<String, Any>()
                            coinMap["id"] = coin.id
                            coinMap["currency"] = coin.currency
                            coinMap["value"] = coin.value
                            // Upload each coin in the wallet to the database
                            coinsDocRef
                                    ?.update(mapOf("$i" to coinMap))
                                    ?.addOnSuccessListener {
                                        Log.i(tag, "${i}th coin has been renewed")
                                        updateValue(wallet)
                                        updateLocal(wallet)
                                    }
                        }
                    }
            bankedNum += 1
            updateBankedNumber()
            return xr * value
        } else {
            Log.i(tag, "No more banking today!")
            Toast.makeText(this, "You have already banked $bankLIMIT coins today!", Toast.LENGTH_SHORT).show()
            return 0.000
        }

    }

    // Banking foreign coin is not limited by bank limit, but it comes with a penalty
    // This promotes player to consider whether banking them directly or using them for the Bonus Trade Offer
    private fun bankForeignCoin(foreignCoinToBank: Coin, penalty: Double = 0.5): Double {

        val xr = exchangeRates[foreignCoinToBank.currency].toString().toDouble()
        val value = foreignCoinToBank.value
        wallet.foreignCoins.remove(foreignCoinToBank)
        updateForeignCoinsListView(wallet)

        // update the online storage of foreign coins
        foreignCoinsDocRef
                ?.set(mapOf()) // First remove everything
                ?.addOnSuccessListener {
                    for (i in 0 until wallet.foreignCoins.size) {
                        val coin = wallet.foreignCoins[i]
                        val coinMap = HashMap<String, Any>()
                        coinMap["id"] = coin.id
                        coinMap["currency"] = coin.currency
                        coinMap["value"] = coin.value
                        // Upload each coin in the wallet to the database
                        foreignCoinsDocRef
                                ?.update(mapOf("$i" to coinMap))
                                ?.addOnSuccessListener {
                                    Log.i(tag, "${i}th foreign coin has been renewed")
                                    updateValue(wallet)
                                    updateForeign(wallet)
                                }

                    }
                }
        return xr * value * penalty // number of gold gained with penalty
    }

}
