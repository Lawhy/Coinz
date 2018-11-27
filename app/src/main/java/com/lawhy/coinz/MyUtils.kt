package com.lawhy.coinz

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.support.v4.content.ContextCompat
import android.util.Log
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MyUtils {
    /**
     * Some handy Utils:
     * 1. getCurrentDate: Current Date in yyyy/MM/dd
     * 2. symbolDrawable: Get Icon according to symbol
     * 3. drawableToIcon: Converting any Drawable to an Icon, for use as a marker icon.
     * 4. combineDrawable: Overlap two drawables and create a new one
     */

    fun getCurrentDate():String {
        val current = LocalDateTime.now()
        val formatter =  DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val currentDate = current.format(formatter)
        Log.i("CurrentDate", currentDate)
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

    fun drawableToIcon(context: Context, vectorDrawable: Drawable): Icon {
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth,
                vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return IconFactory.getInstance(context).fromBitmap(bitmap)
    }

    fun combineDrawable(d1: Drawable?, d2: Drawable?): Drawable {
        val layers = arrayOfNulls<Drawable>(2)
        layers[0] = d1
        layers[1] = d2
        return LayerDrawable(layers)
    }

}