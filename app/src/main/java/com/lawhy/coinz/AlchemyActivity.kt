package com.lawhy.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class AlchemyActivity : AppCompatActivity() {

    /** The AlchemyActivity provides the background story of the core bonus feature of this app:
     *  Suppose a local alchemist has provided recipe that specifies certain combination of the numbers
     *  of coins of the four types. To buy a recipe, the player needs to pay a DEPOSIT (300 500 1000 three choices).
     *  If the player has collected enough combination of coins, he/she can use these coins and throw
     *  them into a CRUCIBLE, the success rate depends on the PURITY of the input coins:
     *     1. PURITY = Average Values of input coins;
     *     2. Success rate = (PURITY * 10) %
     *  So it actually maps the Avg. Values to the Probability of Success. Since the coin value is between 0-10,
     *  this mapping is reasonable.
     *  If it succeeds, the reward would be:
     *     Reward = InputCoinsNumber * MaxValue * HighestExchangeRateToday
     *  where MaxValue = 10 (i.e. the supremum of a coin's value, can be interpreted as all coins "purified").
     *  Notice that this formula can be refined later depending of user's feedback.
     *
     *  This activity helps users to understand how to play with Alchemy and gives user choices of recipe purchase,
     *  the RecipeActivity would be dealing with how to use the purchased recipe.
     * */

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

    // Local wallet
    private lateinit var wallet: Wallet
    // The Alchemical recipe
    private var myRecipe: Recipe = Recipe(0, 0, 0, 0)
    // The highest X-rate today
    private var highestXRate = 0.0

    // View Components
    private lateinit var fabToRecipe: FloatingActionButton
    private lateinit var knowMoreBtn: Button
    private lateinit var walletProgressBar: ProgressBar  // Visible when updating wallet
    private lateinit var hexagramEntrance: ImageView  // Visible after wallet has been prepared

    private val tag = "AlchemyActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alchemy)

        // Read highest exchange rate from Intent
        highestXRate = intent.getDoubleExtra("highestXRate", 0.0)
        if (highestXRate <= 0) {
            Log.d(tag, "Something is wrong, cannot have zero highestXRate.")
            Toast.makeText(this, "Something wrong happened!", Toast.LENGTH_SHORT).show()
            finish()
        }

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

        // Init view components
        walletProgressBar = findViewById(R.id.walletProgressBar)
        hexagramEntrance = findViewById(R.id.hexagramView)
        hexagramEntrance.visibility = View.GONE
        fabToRecipe = findViewById(R.id.fabToRecipe)
        knowMoreBtn = findViewById(R.id.knowMoreBtn)
        setBtnForKnowMoreAboutAlchemy()
    }

    override fun onStart() {
        super.onStart()
        localWallet()
    }

    private fun localWallet() {

        wallet = Wallet(ArrayList(), ArrayList())

        // Retrieve stored coins information from Fire-store
        coinsDocRef?.get()
                ?.addOnSuccessListener {
                    val data = it.data
                    if (data == null || data.isEmpty()) {
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
                            if (foreignData == null || foreignData.isEmpty()) {
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
                        }
                    }
                    // Now the wallet is prepared
                    walletProgressBar.visibility = View.GONE
                    hexagramEntrance.visibility = View.VISIBLE
                    setFabToRecipe() // set this fab here because coins information needs be sent to Recipe Activity
                }
    }

    // Link the knowMoreBtn to a help page specifying the Alchemical process in this game
    private fun setBtnForKnowMoreAboutAlchemy() {

        knowMoreBtn.setOnClickListener {
            // Build a dialog for displaying the help page
            val knowMoreDialogBuilder = AlertDialog.Builder(this@AlchemyActivity)
            val view = View.inflate(this, R.layout.know_more_alchemy_dialog, null)
            knowMoreDialogBuilder.setView(view)
            val knowMoreDialog: AlertDialog = knowMoreDialogBuilder.create()
            val getItBtn: Button = view.findViewById(R.id.getItBtn)
            getItBtn.setOnClickListener {
                knowMoreDialog.dismiss()  // Customized dismiss button
            }
            knowMoreDialog.show()
        }

    }

    // The fabToRecipe button
    private fun setFabToRecipe() {

        fabToRecipe.setOnClickListener {
            val intent = Intent(this, RecipeActivity::class.java)
            intent.putExtra("wallet", wallet)
            intent.putExtra("highestXRate", highestXRate)
            // Before going to see the recipe, we need to ensure there is one, otherwise providing choices to purchase a new one
            recipeDocRef?.get()?.addOnSuccessListener { recipeSnap ->
                val recipe = recipeSnap.data
                if (recipe == null || recipe.isEmpty()) {
                    recipeDocRef?.set(mapOf())
                    recipePurchasing()
                } else {
                    val numSHIL = recipe["SHIL"].toString().toInt()
                    val numDOLR = recipe["DOLR"].toString().toInt()
                    val numQUID = recipe["QUID"].toString().toInt()
                    val numPENY = recipe["PENY"].toString().toInt()
                    myRecipe = Recipe(numSHIL = numSHIL, numDOLR = numDOLR, numQUID = numQUID, numPENY = numPENY)
                    Log.d(tag, "Found existing recipe: $myRecipe")
                    // For existing recipe, go to the RecipeActivity
                    intent.putExtra("recipe", myRecipe)
                    finish()
                    startActivity(intent)
                }
            }
        }
    }

    private fun recipePurchasing() {

        // The purchase choice
        var purchaseChoice = ""

        val recipePurchasingDialogBuilder = AlertDialog.Builder(this@AlchemyActivity)
        val view = View.inflate(this, R.layout.recipe_purchasing_dialog, null)
        recipePurchasingDialogBuilder.setView(view)
        val recipePurchasingDialog = recipePurchasingDialogBuilder.create()
        val normalRecipeBtn: Button = view.findViewById(R.id.normalRecipeBtn)
        val expertRecipeBtn: Button = view.findViewById(R.id.expertRecipeBtn)
        val masterRecipeBtn: Button = view.findViewById(R.id.masterRecipeBtn)
        val purchaseOKBtn: Button = view.findViewById(R.id.purchaseOKBtn)
        // Set animation for selection of a recipe
        normalRecipeBtn.setOnClickListener {
            normalRecipeBtn.background = getDrawable(R.color.orange)
            expertRecipeBtn.background = getDrawable(R.color.transparent)
            masterRecipeBtn.background = getDrawable(R.color.transparent)
            purchaseChoice = "Normal"
        }
        expertRecipeBtn.setOnClickListener {
            normalRecipeBtn.background = getDrawable(R.color.transparent)
            expertRecipeBtn.background = getDrawable(R.color.orange)
            masterRecipeBtn.background = getDrawable(R.color.transparent)
            purchaseChoice = "Expert"
        }
        masterRecipeBtn.setOnClickListener {
            normalRecipeBtn.background = getDrawable(R.color.transparent)
            expertRecipeBtn.background = getDrawable(R.color.transparent)
            masterRecipeBtn.background = getDrawable(R.color.orange)
            purchaseChoice = "Master"
        }
        purchaseOKBtn.setOnClickListener {
            val payment = when (purchaseChoice) {
                "Normal" -> 300.0
                "Expert" -> 500.0
                "Master" -> 1000.0
                else -> 0.0
            }
            if (payment == 0.0) {
                Toast.makeText(this, "Please select one recipe.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // check if there is enough gold
            goldDocRef?.get()?.addOnSuccessListener { g ->
                val goldData = g.data
                var enoughGold = false
                if (goldData == null || goldData.isEmpty()) {
                    Log.d(tag, "Gold account has not been established.")
                } else {
                    val goldInAccount = goldData["goldNumber"].toString().toDouble()
                    if (goldInAccount >= payment) {
                        enoughGold = true
                        Log.d(tag, "Current gold $goldInAccount is enough to purchase the $purchaseChoice recipe.")
                        goldDocRef?.update(mapOf("goldNumber" to (goldInAccount - payment))) // Decrease the gold number in online account accordingly
                    }
                }
                // If enough gold to purchase the recipe
                if (enoughGold) {
                    myRecipe = Recipe.generateNewRecipe(payment)
                    recipeDocRef?.set(myRecipe.toHashMap())?.addOnSuccessListener {
                        Log.d(tag, "Newly purchased recipe has been updated to fire-store.")
                        val intentWithNewRecipe = Intent(this, RecipeActivity::class.java)
                        intentWithNewRecipe.putExtra("wallet", wallet)
                        intentWithNewRecipe.putExtra("highestXRate", highestXRate)
                        intentWithNewRecipe.putExtra("recipe", myRecipe)
                        finish()
                        startActivity(intentWithNewRecipe)
                    }
                } else {
                    Log.d(tag, "No enough gold to purchase.")
                    Toast.makeText(this, "Gold is not enough to purchase.", Toast.LENGTH_SHORT).show()
                }
                recipePurchasingDialog.dismiss()
            }
        }
        recipePurchasingDialog.show()
    }

}
