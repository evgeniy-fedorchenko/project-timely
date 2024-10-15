package com.efedorchenko.timely.service

import android.content.Context
import android.view.Gravity
import android.widget.Toast

object ToastHelper {

    fun showToast(toastText: String, context: Context) {
        val toast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }
}

