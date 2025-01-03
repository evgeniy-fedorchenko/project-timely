package com.efedorchenko.timely.input

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class AddEventDialogFieldsWatcher(
    private val requestNextAfterSymbols: Int,
    private val nextField: EditText
) : TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }

    override fun afterTextChanged(s: Editable?) {
        if (s?.length == requestNextAfterSymbols) {
            nextField.requestFocus()
        }
    }

}