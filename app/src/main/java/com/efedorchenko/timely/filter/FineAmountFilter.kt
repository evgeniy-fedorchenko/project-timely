package com.efedorchenko.timely.filter

import android.text.InputFilter
import android.text.Spanned

class FineAmountFilter : InputFilter {

    companion object {
        private const val MAX_SYMBOLS: Int = 9
        private val ALLOWED: String? = null
        private const val PROHIBITED: String = ""
    }

    override fun filter(
        source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int
    ): CharSequence? {

        return when {
            source.isEmpty() == true -> ALLOWED
            source.matches(Regex("[0-9:]*")) == false -> PROHIBITED
            dest.let { dest.length >= MAX_SYMBOLS } == true -> PROHIBITED
            source.length + dest.length > MAX_SYMBOLS -> PROHIBITED

            else -> ALLOWED
        }
    }
}