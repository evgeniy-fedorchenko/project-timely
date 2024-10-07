package com.efedorchenko.timely.service

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import com.efedorchenko.timely.fragment.AddEventDialog
import com.efedorchenko.timely.fragment.CalendarFragment
import com.efedorchenko.timely.fragment.CalendarFragment.Companion.SELECTED_DATE_KEY
import org.threeten.bp.LocalDate

class CalendarHelper(private val context: Context) {

    companion object {
        fun showToast(toastText: String, context: Context) {
            val toast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }
    }

    fun oldDateSelected(): Toast {
        val toast = Toast.makeText(
            context,
            "Эта дата уже прошла",
            Toast.LENGTH_SHORT
        )
        toast.setGravity(Gravity.CENTER, 0, 0)
        return toast
    }

    fun rejectRewriteEvent(): Toast {
        val toast = Toast.makeText(
            context,
            "Запланированную смену нельзя редактировать!",
            Toast.LENGTH_SHORT
        )
        toast.setGravity(Gravity.CENTER, 0, 0)
        return toast
    }

    fun eventDialog(date: LocalDate, fragment: CalendarFragment, processedCellIdx: Int): AddEventDialog {
        val bundle = Bundle()
        val selectedDateStr = date.toString()
        bundle.putString(SELECTED_DATE_KEY, selectedDateStr)

        val addEventDialog = AddEventDialog.newInstance(fragment, processedCellIdx)
        addEventDialog.arguments = bundle
        return addEventDialog
    }

    fun showToast(toastText: String) {
        val toast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }

}