package com.efedorchenko.timely.filter

import android.text.InputFilter
import android.text.Spanned

class CommentInputFilter : InputFilter {

    override fun filter(
        source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int
    ): CharSequence? {

        return if (dest.length > 500) "" else null
    }
}
