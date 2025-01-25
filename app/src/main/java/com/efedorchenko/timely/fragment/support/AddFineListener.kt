package com.efedorchenko.timely.fragment.support

import com.efedorchenko.timely.model.Fine
import org.threeten.bp.YearMonth

interface AddFineListener {

    fun showAddFineDialog(targetYearMonth: YearMonth)

    fun onSaveFine(newFine: Fine)
}