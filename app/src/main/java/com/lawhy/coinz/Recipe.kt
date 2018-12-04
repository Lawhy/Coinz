package com.lawhy.coinz

import java.io.Serializable
import kotlin.random.Random

class Recipe(
        // The exact number needed in the recipe for each type of coins
        var numSHIL: Int = 0,
        var numDOLR: Int = 0,
        var numQUID: Int = 0,
        var numPENY: Int = 0) : Serializable {

    /** The Recipe class is the key component of the bonus feature: Alchemy play mode, functions here are:
     * 1. generate a new recipe according to input payment
     * 2. transform recipe to a HashMap for online storage
     * 3. modified toString
     *
     * */

    companion object {
        // Random integer bounds for generating the combination of coins
        private var lowestNumber = 0
        private var highestNumber = 0

        fun generateNewRecipe(payment: Double): Recipe {
            when (payment) {
                300.0 -> {
                    lowestNumber = 1; highestNumber = 5
                }
                500.0 -> {
                    lowestNumber = 2; highestNumber = 7
                }
                1000.0 -> {
                    lowestNumber = 3; highestNumber = 10
                }
            }
            return Recipe(numSHIL = Random.nextInt(lowestNumber, highestNumber + 1),  // Plus one because 'until' is not included
                    numDOLR = Random.nextInt(lowestNumber, highestNumber + 1),
                    numQUID = Random.nextInt(lowestNumber, highestNumber + 1),
                    numPENY = Random.nextInt(lowestNumber, highestNumber + 1))
        }
    }

    // This is for fire-store
    fun toHashMap(): HashMap<String, Any> {
        val hashMap = HashMap<String, Any>()
        hashMap["SHIL"] = numSHIL
        hashMap["DOLR"] = numDOLR
        hashMap["QUID"] = numQUID
        hashMap["PENY"] = numPENY
        return hashMap
    }

    override fun toString(): String {
        return "Recipe{ SHIL: $numSHIL, DOLR: $numDOLR, QUID: $numQUID, PENY: $numPENY }"
    }
}