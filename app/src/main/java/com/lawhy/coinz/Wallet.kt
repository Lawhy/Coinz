package com.lawhy.coinz

import java.io.Serializable


class Wallet(val coins: ArrayList<Coin>, val foreignCoins: ArrayList<Coin>) : Serializable {

    /** Uses the Wallet instance to manage coins data collectively.
     * */

    fun add(coin: Coin) {
        this.coins.add(coin)
    }

    fun addForeign(foreignCoin: Coin) {
        this.foreignCoins.add(foreignCoin)
    }

    override fun toString(): String {
        return "Wallet{ Coins: ${coins.size}, Foreign Coins: ${foreignCoins.size} }"
    }

}
