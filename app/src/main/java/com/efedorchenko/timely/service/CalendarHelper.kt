package com.efedorchenko.timely.service

import android.content.Context
import android.view.Gravity
import android.widget.Toast

class CalendarHelper(private val context: Context) {

    companion object {
        fun showToast(toastText: String, context: Context) {
            val toast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }
    }
}