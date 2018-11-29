package com.lawhy.coinz


class Wallet(val coins: ArrayList<Coin>, val foreigncoins: ArrayList<Coin>) {

    fun addAll(coinsList: ArrayList<Coin>) {
        this.coins.addAll(coinsList)
    }

    fun add(coin: Coin) {
        this.coins.add(coin)
    }

    fun addForeign(foreigncoin: Coin) {
        this.foreigncoins.add(foreigncoin)
    }

}
