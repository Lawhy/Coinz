package com.lawhy.coinz


class Wallet(val coins: ArrayList<Coin>, val foreignCoins: ArrayList<Coin>) {

    /** Uses the Wallet instance to manage coins data collectively.
     * */

    fun add(coin: Coin) {
        this.coins.add(coin)
    }

    fun addForeign(foreignCoin: Coin) {
        this.foreignCoins.add(foreignCoin)
    }

}
