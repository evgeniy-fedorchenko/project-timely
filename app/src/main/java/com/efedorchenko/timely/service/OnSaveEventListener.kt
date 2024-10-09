package com.efedorchenko.timely.service

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.efedorchenko.timely.fragment.AddEventDialog
import com.efedorchenko.timely.fragment.CalendarFragment
import com.efedorchenko.timely.model.Event
import org.threeten.bp.LocalDate

abstract class OnSaveEventListener : Fragment() {

    companion object {
        const val SELECTED_DATE_KEY = "selected_date"

        fun eventDialog(date: LocalDate, fragment: CalendarFragment, processedCellIdx: Int): AddEventDialog {
            val bundle = Bundle()
            val selectedDateStr = date.toString()
            bundle.putString(SELECTED_DATE_KEY, selectedDateStr)

            val addEventDialog = AddEventDialog.newInstance(fragment, processedCellIdx)
            addEventDialog.arguments = bundle
            return addEventDialog
        }
    }

    abstract fun onSaveEvent(event: Event, processedCellIdx: Int?)

}