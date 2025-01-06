package com.efedorchenko.timely.fragment.support

import com.efedorchenko.timely.model.Fine
import org.threeten.bp.Month

interface AddFineListener {

    fun showAddFineDialog(targetMonth: Month)

    fun onSaveFine(newFine: Fine)
}