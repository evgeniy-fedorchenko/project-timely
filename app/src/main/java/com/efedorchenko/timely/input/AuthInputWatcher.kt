package com.efedorchenko.timely.input

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.efedorchenko.timely.R

class AuthInputWatcher(
    val textToWatch: EditText,
    val validator: (String) -> Boolean
) : TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
        val input = s.toString()
        if (input.isEmpty()) {
            textToWatch.setBackgroundResource(R.drawable.auth_form_background)
        } else {
            val isValid = validator(input)
            textToWatch.setBackgroundResource(
                if (isValid) R.drawable.auth_form_background
                else R.drawable.auth_form_background_error
            )
        }
    }
}