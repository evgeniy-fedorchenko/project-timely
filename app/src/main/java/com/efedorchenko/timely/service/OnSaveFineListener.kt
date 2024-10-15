package com.efedorchenko.timely.service

import androidx.fragment.app.Fragment
import com.efedorchenko.timely.fragment.AddFineDialog
import com.efedorchenko.timely.fragment.SummaryFragment
import com.efedorchenko.timely.model.Fine
import org.threeten.bp.Month

abstract class OnSaveFineListener : Fragment() {

    companion object {
        const val SELECTED_DATE_KEY = "selected_date"
        const val ADD_FINE_DIALOG_TAG = "add_fine_dialog"


        fun fineDialog(targetMonth: Month, fragment: SummaryFragment): AddFineDialog {
            return AddFineDialog.newInstance(fragment, targetMonth)
        }
    }

    abstract fun onSaveFine(newFine: Fine)

}