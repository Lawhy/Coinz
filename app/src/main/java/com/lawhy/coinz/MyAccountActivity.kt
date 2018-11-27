package com.lawhy.coinz

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class MyAccountActivity : AppCompatActivity() {

    private lateinit var exchangeRates: HashMap<String, Any>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_account)
    }
    //这边留给MyAccount
//                        for (coinMap in data) {
//                            Log.d("[userID:$userID]", coinMap.toString())
//                            val id = coinMap.key as String
//                            val properties = coinMap.value as HashMap<*, *>
//                            val currency = properties["currency"] as String
//                            val value = properties["value"] as Double
//                            val coin = Coin(id, currency, value, null) // Here marker is not important
//                            Log.d("[coinID:$id]", "Currency:$currency Value:$value")
//                            wallet.add(coin)
//                        }

}
