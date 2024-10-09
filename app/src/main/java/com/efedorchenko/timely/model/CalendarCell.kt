package com.efedorchenko.timely.model

import android.view.View.OnClickListener

data class CalendarCell(
    val text: String,
    val textStyle: Int,
    val parentBackground: Int,
    val onClickListener: OnClickListener?,
    val event: Event?,
)