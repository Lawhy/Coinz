package com.lawhy.coinz

class Wallet(coins: ArrayList<Coin>) {

    var coins : ArrayList<Coin> = coins

    fun addCoin(coin: Coin) {
        this.coins.add(coin)
    }
}
