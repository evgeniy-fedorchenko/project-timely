package com.efedorchenko.timely.filter

import android.text.InputFilter
import android.text.Spanned

class CommentInputFilter : InputFilter {

    companion object {
        private const val MAX_SYMBOLS = 70
    }

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {

        val dLen = dest.length
        return if (dLen >= MAX_SYMBOLS || dLen + source.length >= MAX_SYMBOLS) "" else null
    }
}
