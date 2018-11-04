package com.lawhy.coinz;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;

public class IconUtils {
    /**
     * Some handy Utils:
     * 1. drawableToIcon: Converting any Drawable to an Icon, for use as a marker icon.
     * 2. combineDrawable: Overlap two drawables and create a new one
     */


    public static Icon drawableToIcon(@NonNull Context context, Drawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return IconFactory.getInstance(context).fromBitmap(bitmap);
    }


    public static Drawable combineDrawable(Drawable d1, Drawable d2) {
        Drawable[] layers = new Drawable[2];
        layers[0] = d1;
        layers[1] = d2;
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        return layerDrawable;
    }

}
