package com.lawhy.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
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

class RecipeActivity : AppCompatActivity() {

    /** This activity mainly displays the purchased recipe and user can fill in
     * any combination of coins to fulfill the recipe. But no more coins can be
     * added when the requirement has been met, upon that, user can click the
     * {refine button} and wait for the result to be shown. As told before, the
     * {Purity (Average values of the input coins)} of the combination defines how
     * probable the refinement would succeed. User can see the Purity change when-
     * ever he/she selects or deselects the coin. After the combination of coins
     * is submitted, all the used coins will be deleted from account and if there
     * is a reward, the gold number will increase correspondingly.
     *
     * To summarise:
     *     1. PURITY = Average Values of input coins;
     *     2. Succeed when a random number (0 to 1) * 10 <= PURITY
     *     3. Reward = InputCoinsNumber * MaxValue * HighestExchangeRateToday where MaxValue = 10,
     *     an interpretation here is that all coins are "purified".
     *
     * Possible Improvement: Since this activity involves with a lot dynamic process such as select
     * -ing coins, update Purity in real-time, and so on, it is very likely to refine the code and
     * make everything faster.
     * */

    // View components
    // Fab for adding coins to the recipe
    private lateinit var fabSHIL: FloatingActionButton
    private lateinit var fabDOLR: FloatingActionButton
    private lateinit var fabQUID: FloatingActionButton
    private lateinit var fabPENY: FloatingActionButton
    // Displaying added number of coins
    private lateinit var filledSHIL: TextView
    private lateinit var filledDOLR: TextView
    private lateinit var filledQUID: TextView
    private lateinit var filledPENY: TextView
    // Displaying required number of coins
    private lateinit var requiredSHIL: TextView
    private lateinit var requiredDOLR: TextView
    private lateinit var requiredQUID: TextView
    private lateinit var requiredPENY: TextView
    // Displaying the purity, i.e. Purity = (Average value of the input coins * 10) %
    private lateinit var purityView: TextView
    // Refine Btn: for submitting the selected combination of coins and throw them into a crucible
    private lateinit var refineBtn: Button

    // Fire-base
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private lateinit var userID: String
    private lateinit var userEmail: String
    private var firestore: FirebaseFirestore? = null
    private var recipeDocRef: DocumentReference? = null
    private var coinsDocRef: DocumentReference? = null
    private var foreignCoinsDocRef: DocumentReference? = null
    private var goldDocRef: DocumentReference? = null

    // local wallet and current ongoing recipe
    private lateinit var wallet: Wallet
    private lateinit var myRecipe: Recipe

    // The highest X-Rate today
    private var highestXRate: Double = 0.0

    // Essential attributes for a recipe
    private var purity = 0.0
    // Selected local coins
    private val selectedLocalSHIL = ArrayList<Coin>()
    private val selectedLocalDOLR = ArrayList<Coin>()
    private val selectedLocalQUID = ArrayList<Coin>()
    private val selectedLocalPENY = ArrayList<Coin>()
    // Selected foreign coins
    private val selectedForeignSHIL = ArrayList<Coin>()
    private val selectedForeignDOLR = ArrayList<Coin>()
    private val selectedForeignQUID = ArrayList<Coin>()
    private val selectedForeignPENY = ArrayList<Coin>()
    // Overall selection
    private val selectionLocal = ArrayList<Coin>()
    private val selectionForeign = ArrayList<Coin>()
    // Check Box linked with coin
    private val mapOfCoinToCheckBox = HashMap<Coin, CheckBox>()


    private val tag = "RecipeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe)

        // Receive local wallet, highestXRate, and myRecipe from AlchemyActivity
        wallet = intent.getSerializableExtra("wallet") as Wallet
        Log.d(tag, wallet.toString())
        highestXRate = intent.getDoubleExtra("highestXRate", 0.0)
        myRecipe = intent.getSerializableExtra("recipe") as Recipe
        Log.d(tag, myRecipe.toString())

        // Init the view components
        fabSHIL = findViewById(R.id.fabSHIL)
        fabDOLR = findViewById(R.id.fabDOLR)
        fabQUID = findViewById(R.id.fabQUID)
        fabPENY = findViewById(R.id.fabPENY)
        filledSHIL = findViewById(R.id.filledSHIL)
        filledDOLR = findViewById(R.id.filledDOLR)
        filledQUID = findViewById(R.id.filledQUID)
        filledPENY = findViewById(R.id.filledPENY)
        requiredSHIL = findViewById(R.id.requiredSHIL)
        requiredDOLR = findViewById(R.id.requiredDOLR)
        requiredQUID = findViewById(R.id.requiredQUID)
        requiredPENY = findViewById(R.id.requiredPENY)
        purityView = findViewById(R.id.purityView)
        purityView.text = "0" // 0% purity at start
        refineBtn = findViewById(R.id.refineBtn)

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

        // Init document reference
        recipeDocRef = firestore?.collection("recipe")?.document(userEmail)
        coinsDocRef = firestore?.collection("coins")?.document(userEmail)
        foreignCoinsDocRef = firestore?.collection("foreignCoins")?.document(userEmail)
        goldDocRef = firestore?.collection("gold")?.document(userEmail)


    }

    override fun onStart() {
        super.onStart()
        initRecipeRequirement()
        setFabForFillingCoins("SHIL")
        setFabForFillingCoins("DOLR")
        setFabForFillingCoins("QUID")
        setFabForFillingCoins("PENY")
        // Set up the refine button for submitting the input coins
        refineBtn.setOnClickListener {
            val resultDialogBuilder = AlertDialog.Builder(this)
            val view = View.inflate(this, R.layout.alchemy_result_dialog, null)
            val resultDescription: TextView = view.findViewById(R.id.resultDescription)
            val exitBtn: Button = view.findViewById(R.id.resultExitBtn)
            resultDialogBuilder.setView(view)
            val resultDialog = resultDialogBuilder.create()
            if (isRequirementMet()) {
                val reward = goldRefine(adjustment = 0.0)
                if (reward > 0) {
                    resultDescription.text = ("Congratulation! \n The recipe works and all the \n coins are refined to purest \n " +
                            "GOLD! (Reward: %.3f)").format(reward)
                }
                resultDialog.show()
            } else {
                Toast.makeText(this, "You have not fulfilled the recipe...", Toast.LENGTH_SHORT).show()
            }
            exitBtn.setOnClickListener { resultDialog.dismiss() }
            resultDialog.setOnDismissListener {
                finish() // Go back to MapActivity when the result has been read.
            }
        }
    }

    private fun initRecipeRequirement() {
        // Display the number of required coins in the recipe
        requiredSHIL.text = myRecipe.numSHIL.toString()
        requiredDOLR.text = myRecipe.numDOLR.toString()
        requiredQUID.text = myRecipe.numQUID.toString()
        requiredPENY.text = myRecipe.numPENY.toString()
    }

    private fun setFabForFillingCoins(currency: String) {
        // Assign fab according to currency type
        val fab = when (currency) {
            "SHIL" -> fabSHIL
            "DOLR" -> fabDOLR
            "QUID" -> fabQUID
            "PENY" -> fabPENY
            else -> Button(this)
        }
        // Assign the required number of coins for this currency type
        val requiredNumOfCoins = when (currency) {
            "SHIL" -> myRecipe.numSHIL
            "DOLR" -> myRecipe.numDOLR
            "QUID" -> myRecipe.numQUID
            "PENY" -> myRecipe.numPENY
            else -> 0
        }
        // Assign the view of filled number of coins for this currency type
        val filledNumView = when (currency) {
            "SHIL" -> filledSHIL
            "DOLR" -> filledDOLR
            "QUID" -> filledQUID
            "PENY" -> filledPENY
            else -> TextView(this)
        }

        // List of local coins of the specified currency
        val coinsOfCurrency = ArrayList<Coin>()
        for (coin in wallet.coins) {
            if (coin.currency == currency) {
                coinsOfCurrency.add(coin)
            }
        }
        // List of foreign coins of the specified currency
        val foreignCoinsOfCurrency = ArrayList<Coin>()
        for (foreignCoin in wallet.foreignCoins) {
            if (foreignCoin.currency == currency) {
                foreignCoinsOfCurrency.add(foreignCoin)
            }
        }
        // set selection list of local and foreign coins
        val selectionLocalCoinsOfCurrency = when (currency) {
            "SHIL" -> selectedLocalSHIL
            "DOLR" -> selectedLocalDOLR
            "QUID" -> selectedLocalQUID
            "PENY" -> selectedLocalPENY
            else -> ArrayList()
        }
        val selectionForeignCoinsOfCurrency = when (currency) {
            "SHIL" -> selectedForeignSHIL
            "DOLR" -> selectedForeignDOLR
            "QUID" -> selectedForeignQUID
            "PENY" -> selectedForeignPENY
            else -> ArrayList()
        }
        // set fab onclick
        fab.setOnClickListener {
            // Dialog for selecting local and foreign coins
            val selectCoinsDialogBuilder = AlertDialog.Builder(this@RecipeActivity)
            val view = View.inflate(this, R.layout.choose_coins_to_refine_dialog, null)
            selectCoinsDialogBuilder.setView(view)

            // Display the coins in two separate lists
            val localCoinsList: TableLayout = view.findViewById(R.id.selectLocalCoins)
            val foreignCoinsList: TableLayout = view.findViewById(R.id.selectForeignCoins)
            if (coinsOfCurrency.isNotEmpty()) localCoinsList.removeAllViews() // Remove "Nothing" Text View
            if (foreignCoinsOfCurrency.isNotEmpty()) foreignCoinsList.removeAllViews() // Remove "Nothing" Text View

            // The confirm selection fab
            val fabConfirmSelection: FloatingActionButton = view.findViewById(R.id.fabConfrimSelection)

            // The selection counter
            val selectionCount: TextView = view.findViewById(R.id.selectionCount)

            // Display the local coins
            displayCoinsOfCurrency(currency,
                    coinsOfCurrency,
                    localCoinsList,
                    selectionCount,
                    selectionLocalCoinsOfCurrency,
                    selectionForeignCoinsOfCurrency,
                    requiredNumOfCoins,
                    isLocal = true)
            // Display the foreign coins
            displayCoinsOfCurrency(currency,
                    foreignCoinsOfCurrency,
                    foreignCoinsList,
                    selectionCount,
                    selectionLocalCoinsOfCurrency,
                    selectionForeignCoinsOfCurrency,
                    requiredNumOfCoins,
                    isLocal = false)

            // The view is set ready now.
            val selectCoinsDialog = selectCoinsDialogBuilder.create()
            selectCoinsDialog.setOnDismissListener {
                // On confirmation, the view of filled number of coins of the currency should be updated
                filledNumView.text = "${selectionLocalCoinsOfCurrency.size + selectionForeignCoinsOfCurrency.size}"
                updatePurity()

            }
            fabConfirmSelection.setOnClickListener {
                selectCoinsDialog.dismiss()
            }
            selectionCount.text = "Selected: ${selectionLocalCoinsOfCurrency.size + selectionForeignCoinsOfCurrency.size}"
            selectCoinsDialog.show()
        }
    }

    private fun displayCoinsOfCurrency(currency: String,
                                       coinsOfCurrency: ArrayList<Coin>,
                                       coinListLayout: TableLayout,
                                       selectionCount: TextView,
                                       selectionLocalCoinsOfCurrency: ArrayList<Coin>,
                                       selectionForeignCoinsOfCurrency: ArrayList<Coin>,
                                       requiredNumOfCoins: Int,
                                       isLocal: Boolean = true) {
        var i = 0
        for (coin in coinsOfCurrency) {
            val tr = TableRow(this)
            tr.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
            tr.setPadding(8, 8, 8, 8)
            tr.gravity = Gravity.CENTER
            // Display the coin information
            val textCoin = TextView(this)
            textCoin.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT)
            textCoin.gravity = Gravity.CENTER
            textCoin.isClickable = true
            textCoin.textSize = 16f
            textCoin.text = "$currency[$i]: %.3f".format(coin.value)
            i += 1
            // Display the checkbox for selection, needed to be check if already created
            val checkCoin = CheckBox(this)
            checkCoin.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT)
            checkCoin.text = ""
            // Make sure the selected coin won't be counted twice
            if (mapOfCoinToCheckBox[coin] != null) {
                checkCoin.isChecked = mapOfCoinToCheckBox[coin]?.isChecked!!
            }

            tr.addView(textCoin)
            tr.addView(checkCoin)
            coinListLayout.addView(tr)
            mapOfCoinToCheckBox[coin] = checkCoin

            // Set checkBox for each coin
            checkCoin.setOnClickListener {

                // Coins that are checked should be added to selection.
                if (checkCoin.isChecked) {

                    // For enough selection
                    if (requiredNumOfCoins <= selectionLocalCoinsOfCurrency.size + selectionForeignCoinsOfCurrency.size) {
                        checkCoin.isChecked = false
                        Toast.makeText(this, "You have selected enough coins.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    if (isLocal) {
                        selectionLocalCoinsOfCurrency.add(coin)
                    } else {
                        selectionForeignCoinsOfCurrency.add(coin)
                    }
                    Log.d(tag, "Select $coin.")
                } else {

                    // Coins that are unchecked should be removed to selection.
                    if (isLocal) {
                        selectionLocalCoinsOfCurrency.remove(coin)
                    } else {
                        selectionForeignCoinsOfCurrency.remove(coin)
                    }
                    Log.d(tag, "Remove $coin from selection.")
                }
                selectionCount.text = "Selected: ${selectionLocalCoinsOfCurrency.size + selectionForeignCoinsOfCurrency.size}"
            }
        }

    }

    private fun renewSelection() {
        // Init overall selection
        selectionLocal.clear()
        selectionLocal.addAll(selectedLocalSHIL)
        selectionLocal.addAll(selectedLocalDOLR)
        selectionLocal.addAll(selectedLocalQUID)
        selectionLocal.addAll(selectedLocalPENY)
        selectionForeign.clear()
        selectionForeign.addAll(selectedForeignSHIL)
        selectionForeign.addAll(selectedForeignDOLR)
        selectionForeign.addAll(selectedForeignQUID)
        selectionForeign.addAll(selectedForeignPENY)
    }

    private fun updatePurity() {
        renewSelection()
        var totalValue = 0.0
        val totalSize = selectionLocal.size + selectionForeign.size
        for (coin in selectionLocal) {
            totalValue += coin.value
        }
        for (coin in selectionForeign) {
            totalValue += coin.value
        }
        purity = (totalValue / totalSize) * 10
        purityView.text = "%.1f".format(purity)
        Log.d(tag, "The current purity is $purity %")
        Log.d(tag, "The current selection number $totalSize")
        Log.d(tag, "Local: $selectionLocal")
        Log.d(tag, "Foreign: $selectionForeign")
    }

    private fun isRequirementMet(): Boolean {
        return filledSHIL.text == myRecipe.numSHIL.toString() &&
                filledDOLR.text == myRecipe.numDOLR.toString() &&
                filledQUID.text == myRecipe.numQUID.toString() &&
                filledPENY.text == myRecipe.numPENY.toString()
    }

    // Can only be invoked when requirement is met, the adjustment is to increase the probability of success
    private fun goldRefine(adjustment: Double = 0.01): Double {
        var reward = 0.0
        // Get a random number that decides our result, then adjust it a little bit
        val random: Double = Math.random() - adjustment
        Log.d(tag, "The random number is %.3f and Purity is %.3f".format(random, purity))
        // If the random number * 10 is not covered by purity
        if (random * 100 > purity) {
            Log.d(tag, "Complete Failure !!!")
        } else {
            Log.d(tag, "Complete Success !!!")
            // Count the reward
            val numSelectedCoins = selectionLocal.size + selectionForeign.size
            Log.d(tag, "NUM SELECTED COINS: $numSelectedCoins")
            // Reward = (number of coins) * (highest exchange rate today) * (maximum value of a coin can have)
            // The interpretation is that every coin is refined to the purest gold!
            reward = numSelectedCoins * highestXRate * 10
            Log.d(tag, "Reward: $numSelectedCoins coins * %.3f * 10".format(highestXRate))
            // Increase the gold number by the reward
            goldDocRef?.get()?.addOnSuccessListener {
                val goldData = it.data
                if (goldData.isNullOrEmpty()) {
                    val map = HashMap<String, Any>()
                    map["bankedNumber"] = 0
                    map["goldNumber"] = reward
                    goldDocRef?.set(map)
                    Log.d(tag, "Initialize gold number $reward to the online account.")
                } else {
                    val newGoldNumber = reward + goldData["goldNumber"].toString().toDouble()
                    Log.d(tag, "New Gold number in account: $newGoldNumber")
                    goldDocRef?.update(mapOf("goldNumber" to newGoldNumber))
                }
            }
        }
        renewWalletAfterGoldRefinement()
        // Clean the recipe as well
        recipeDocRef?.set(mapOf())?.addOnSuccessListener { Log.d(tag, "Recipe has been used.") }
        return reward
    }

    // Update the rest of coins to the fire-store
    private fun renewWalletAfterGoldRefinement() {
        // Record the before and after number of coins in the wallet.
        val before = wallet.coins.size + wallet.foreignCoins.size
        wallet.coins.removeAll(selectionLocal)
        wallet.foreignCoins.removeAll(selectionForeign)
        val after = wallet.coins.size + wallet.foreignCoins.size
        Log.d(tag, "${before - after} coins have been removed.")
        Log.d(tag, "Local coins rest: ${wallet.coins}")
        Log.d(tag, "Foreign coins rest: ${wallet.foreignCoins}")
        Toast.makeText(this, "${before - after} coins were used for the recipe.", Toast.LENGTH_SHORT).show()
        updateWalletToFireStore()
    }

    private fun updateWalletToFireStore() {

        // update the online storage of local coins
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
                                }
                    }
                }

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
                                }

                    }
                }

    }

}
