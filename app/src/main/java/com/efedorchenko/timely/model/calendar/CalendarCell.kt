package com.efedorchenko.timely.model.calendar

import android.view.View.OnClickListener
import com.efedorchenko.timely.model.Event

data class CalendarCell(

    val text: String,
    val textStyle: Int,
    val background: Int,
    val onClickListener: OnClickListener?,
    val event: Event?
)