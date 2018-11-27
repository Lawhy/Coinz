package com.lawhy.coinz


class Wallet(var coins: ArrayList<Coin>) {

    fun addAll(coinsList: ArrayList<Coin>) {
        this.coins.addAll(coinsList)
    }

    fun add(coin: Coin) {
        this.coins.add(coin)
    }

}
