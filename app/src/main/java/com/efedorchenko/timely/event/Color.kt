package com.efedorchenko.timely.event

import android.content.Context
import androidx.core.content.ContextCompat
import com.efedorchenko.timely.R

enum class Color(private val colorResId: Int) {
    RED(R.color.red),
    GREEN(R.color.green),
    ORANGE(R.color.orange);

    fun getColorValue(context: Context): Int = ContextCompat.getColor(context, colorResId)
}