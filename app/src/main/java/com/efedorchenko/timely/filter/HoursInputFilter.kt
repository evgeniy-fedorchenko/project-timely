package com.efedorchenko.timely.filter

import android.text.InputFilter
import android.text.Spanned

class HoursInputFilter : InputFilter {

    companion object {
        private val VALID_PAST_TWO: List<CharSequence> = listOf("0", "1", "2", "3")
        private val ALLOWED: String? = null
        private const val PROHIBITED: String = ""
    }

    override fun filter(
        source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int
    ): CharSequence? {

        return when {
            dest?.isEmpty() == true -> ALLOWED
            source?.matches(Regex("[0-9:]*")) == false -> PROHIBITED
            else ->
                when (source?.length?.let { dest?.length?.plus(it) }) {
                    1 -> ALLOWED
                    2 -> if ("0" == dest.toString()
                        || "1" == dest.toString()
                        || ("2" == dest.toString() && VALID_PAST_TWO.contains(source))
                    ) {
                        ALLOWED
                    } else PROHIBITED

                    else -> PROHIBITED
                }
        }
    }

}
