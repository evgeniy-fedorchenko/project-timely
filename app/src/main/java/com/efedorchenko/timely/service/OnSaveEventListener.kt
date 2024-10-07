package com.efedorchenko.timely.service

import com.efedorchenko.timely.data.Event

interface OnSaveEventListener {

    fun onSaveEvent(event: Event, processedCellIdx: Int?)

}