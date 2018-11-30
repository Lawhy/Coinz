package com.lawhy.coinz

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Gravity
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.time.LocalDateTime

class MyAccountActivity : AppCompatActivity() {

    private lateinit var exchangeRates: HashMap<*, *>
    private lateinit var wallet: Wallet
    private var goldNumber: Double = 0.000
    private var bankedNum = 0
    private val bankLIMIT = 25
    private var mapDate = MyUtils().getCurrentDate()

    /* The statistics summary of Gold and Coins
     * 1. Number of Gold
     * 2. For each type of the currency {SHIL, DOLR, QUID, PENY}:
     *    a. Number of local coins
     *    b. Number of foreign coins (Received from other players)
     *    c. Value (In total)
     *    d. Exchange-Rate (In-time X-Rate)
     */
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

    // Display banked number of coins today
    private lateinit var bankNumberView: TextView

    // Firebase
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private lateinit var userID: String
    private lateinit var userEmail: String
    private var firestore: FirebaseFirestore? = null

    private val tag = "MyAccountActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_account)

        exchangeRates = intent.extras?.get("exchangeRates") as HashMap<*, *>

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
        localQUID = findViewById(R.id.localQUID )
        foreignQUID  = findViewById(R.id.foreignQUID )
        valQUID  = findViewById(R.id.valQUID )
        xrQUID  = findViewById(R.id.xrQUID )

        // PENY
        localPENY = findViewById(R.id.localPENY)
        foreignPENY = findViewById(R.id.foreignPENY)
        valPENY= findViewById(R.id.valPENY)
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

        firestore?.collection("gold")
                ?.document(userEmail)
                ?.get()
                ?.addOnSuccessListener {
                    val data = it.data
                    if (data.isNullOrEmpty()) {
                        Log.i(tag,"No gold/bank number information.")
                        val originalMap = HashMap<String, Any>()
                        originalMap["goldNumber"] = 0.000
                        originalMap["bankedNumber"] = 0
                        firestore?.collection("gold")
                                ?.document(userEmail)
                                ?.set(originalMap)

                    } else {
                        if(mapDate != MyUtils().getCurrentDate()) {
                            Log.i(tag, "Renew banked number for a new day!")
                            mapDate = MyUtils().getCurrentDate()
                            bankedNum = 0
                            firestore?.collection("gold")
                                    ?.document(userEmail)
                                    ?.update(mapOf("bankedNumber" to 0))
                        } else {
                            bankedNum = if (data.keys.contains("bankedNumber"))
                            { data["bankedNumber"].toString().toInt() } else { 0 }
                            Log.i(tag, "The banked coin number is $bankedNum")
                        }
                    }
                    bankNumberView.text = "You have banked $bankedNum coins today (Limit: 25)."
                }
    }

    private fun updateBankedNumber() {

        bankNumberView.text = "You have banked $bankedNum coins today (Limit: 25)."

        firestore?.collection("gold")
                ?.document(userEmail)
                ?.update(mapOf("bankedNumber" to bankedNum))
                ?.addOnSuccessListener{ Log.i(tag, "Banked Coin Number has been uploaded to Database.")}
                ?.addOnFailureListener { Log.wtf(tag, "Banked Coin Number cannot be uploaded!") }

    }

    private fun initGoldStat() {

        // Retrieve stored Gold Number from Fire-store
        firestore?.collection("gold")
                ?.document(userEmail)
                ?.get()
                ?.addOnSuccessListener {
                    val data = it.data
                    if (data.isNullOrEmpty()) {
                        Log.i(tag, "No Gold Info in the bank account.")
                        val originalMap = HashMap<String, Any>()
                        originalMap["goldNumber"] = 0.000
                        originalMap["bankedNumber"] = 0
                        firestore?.collection("gold")
                                ?.document(userEmail)
                                ?.set(originalMap)
                                ?.addOnSuccessListener { Log.i(tag, "Init Gold Number in account.") }
                                ?.addOnFailureListener { Log.wtf(tag, "Gold Number cannot be initialised!") }
                    } else  {
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
        firestore?.collection("gold")
                ?.document(userEmail)
                ?.update(mapOf("goldNumber" to goldNumber))
                ?.addOnSuccessListener { Log.i(tag, "Gold Number has been updated!")}
                ?.addOnFailureListener { Log.wtf(tag, "Gold Number cannot be updated!") }

        goldNumberView.text = "%.3f".format(goldNumber)

    }

    private fun localWallet() {

        wallet = Wallet(ArrayList(), ArrayList())

        // Retrieve stored coins information from Fire-store
        firestore?.collection("coins")
                ?.document(userEmail)
                ?.get()
                ?.addOnSuccessListener {
                    val data = it.data
                    if (data.isNullOrEmpty()) {
                        Log.i(tag, "No coins in the wallet.")
                    } else {
                        for (coinMap in data) {
                            Log.i("[userEmail:$userEmail]", "Local Wallet is initialised.")
                            val index = coinMap.key as String
                            val properties = coinMap.value as HashMap<*, *>
                            val id = properties["id"] as String
                            val currency = properties["currency"] as String
                            val value = properties["value"] as Double
                            val coin = Coin(id, currency, value, null) // Here marker is not important
                            Log.i("[coin$index]", "$currency: $value")
                            wallet.add(coin) // These coins are all local coins
                        }
                        // Update everything when wallet is prepared
                        updateXRates()
                        updateValue(wallet)
                        updateLocal(wallet)
                        updateCoinsListView(wallet)
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

        for (coin in wallet.coins) {
            val currency = coin.currency
            when(currency) {
                "SHIL" -> {valS += coin.value}
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
            when(currency) {
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

    private fun updateCoinsListView(wallet: Wallet) {

        coinsListView.removeAllViews()

        for (i in 0 until wallet.coins.size) {
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
            tvCurrency.text = wallet.coins[i].currency
            tvCurrency.gravity = Gravity.CENTER

            // Display value
            val tvValue = TextView(this)
            tvValue.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 3F)
            tvValue.text = "%.3f".format(wallet.coins[i].value)
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

            // Add Set OnClick Listener
            bankBtn.setOnClickListener {
                val coin = wallet.coins[i]
                val alertDialog = AlertDialog.Builder(this)
                alertDialog.setTitle("Sure to bank this coin?")
                alertDialog.setMessage("${coin.currency}: %.3f".format(coin.value))
                alertDialog.setPositiveButton("YES") { _,_ ->
                    val gain = bankCoin(coin)
                    if(gain > 0) {
                        Log.i(tag, "Gained Gold $gain")
                        updateGoldStat(increase = gain)
                        updateCoinsListView(wallet)
                    } else {
                        Log.i(tag, "Coin is not allowed to banked.")
                    }
                }
                alertDialog.setNegativeButton("NO") {_,_ -> }
                alertDialog.show()
            }

            sendBtn.setOnClickListener {
               // val sentCoin = wallet.coins[i]
            }
        }
    }

    private fun bankCoin(coinToBank: Coin): Double {

        // Bank only happens before exceeding the limit of 25 coins
        if(bankedNum <= bankLIMIT) {
            val xr = exchangeRates[coinToBank.currency].toString().toDouble()
            val value = coinToBank.value
            wallet.coins.remove(coinToBank)
            // Replacing the online wallet by local wallet
            firestore?.collection("coins")
                    ?.document(userEmail)
                    ?.set(mapOf()) // First remove everything
                    ?.addOnSuccessListener {
                        for (i in 0 until wallet.coins.size) {
                            val coin = wallet.coins[i]
                            val coinMap = HashMap<String, Any>()
                            coinMap["id"] = coin.id
                            coinMap["currency"] = coin.currency
                            coinMap["value"] = coin.value
                            // Upload each coin in the wallet to the database
                            firestore?.collection("coins")
                                    ?.document(userEmail)
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

//    private fun bankForeignCoin(id: Int, penalty: Double = 0.5): Double {
//        val foreignCoinToBank = wallet.foreignCoins[id]
//        val xr = exchangeRates[foreignCoinToBank.currency].toString().toDouble()
//        val value = foreignCoinToBank.value
//        return xr * value * penalty // number of gold gained with penalty
//    }

}
