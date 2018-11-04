package com.lawhy.coinz

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/* Just some utility functions that might be used somewhere in the app */

class MyUtil {

    fun getCurrentDate():String {
        val current = LocalDateTime.now()
        val formatter =  DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val currentDate = current.format(formatter)
        Log.i("CurrentDate", "$currentDate")
        return currentDate.trim()
    }

    fun symbolDrawable(context: Context, symbol: Int): Drawable?{
        var icNumber : Drawable? = null
        when(symbol) {
            0 -> icNumber = ContextCompat.getDrawable(context, R.drawable.ic_zero)
            1 -> icNumber = ContextCompat.getDrawable(context, R.drawable.ic_one)
            2 -> icNumber = ContextCompat.getDrawable(context, R.drawable.ic_two)
            3 -> icNumber = ContextCompat.getDrawable(context, R.drawable.ic_three)
            4 -> icNumber = ContextCompat.getDrawable(context, R.drawable.ic_four)
            5 -> icNumber = ContextCompat.getDrawable(context, R.drawable.ic_five)
            6 -> icNumber = ContextCompat.getDrawable(context, R.drawable.ic_six)
            7 -> icNumber = ContextCompat.getDrawable(context, R.drawable.ic_seven)
            8 -> icNumber = ContextCompat.getDrawable(context, R.drawable.ic_eight)
            9 -> icNumber = ContextCompat.getDrawable(context, R.drawable.ic_nine)
            else -> Log.d(context.packageName, "Invalid number on a coin is detected!")
        }
        return icNumber
    }

}