package com.lawhy.coinz


class Wallet(val coins: ArrayList<Coin>, val foreignCoins: ArrayList<Coin>) {


    fun add(coin: Coin) {
        this.coins.add(coin)
    }

    fun addForeign(foreigncoin: Coin) {
        this.foreignCoins.add(foreigncoin)
    }

}
