package com.lawhy.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class AlchemyActivity : AppCompatActivity() {

    /** This activity provides the core bonus feature of this app, i.e. Alchemy. The background story
     *  is that local alchemist has provided recipe that specifies certain combination of the numbers
     *  of coins of the four types. To buy a recipe, the player needs to pay a DEPOSIT (500 GOLD for now).
     *  If the player has collected enough combination of coins, he/she can use these coins and throw
     *  them into a CRUCIBLE, the success rate depends on the PURITY of the input coins:
     *     1. PURITY = Average Values of input coins;
     *     2. Success rate = (PURITY * 10) %
     *  So it actually maps the Avg. Values to the Probability of Success. Since the coin value is between 0-10,
     *  this mapping is reasonable.
     *  If it succeeds, the reward would be:
     *     Reward = InputCoinsNumber * MaxValue * HighestExchangeRateToday
     *  where MaxValue = 10 (i.e. the supremum of a coin's value).
     *  Notice that this formula can be refined later depending of user's feedback.
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

    // Local wallet
    private lateinit var wallet: Wallet

    // View Components
    private lateinit var fabToRecipe: FloatingActionButton
    private lateinit var knowMoreBtn: Button

    private val tag = "AlchemyActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alchemy)

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

        // Init view components
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
                            // Now the wallet is prepared
                            //
                        }
                    }
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


}
