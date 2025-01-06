package com.efedorchenko.timely.fragment.support

import com.efedorchenko.timely.model.Event
import org.threeten.bp.LocalDate

interface AddEventListener {

    fun showAddEventDialog(targetDate: LocalDate)

    fun onSaveEvent(event: Event)
}