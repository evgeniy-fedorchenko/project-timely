package com.efedorchenko.timely.service

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.efedorchenko.timely.data.Event
import com.efedorchenko.timely.data.Fine
import com.efedorchenko.timely.data.MonthUID
import com.efedorchenko.timely.repository.EventRepository
import com.efedorchenko.timely.repository.FineRepository

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val fineRepository: FineRepository = FineRepository(application)
    private val eventRepository: EventRepository = EventRepository(application)

    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> get() = _events

    private val _fines = MutableLiveData<List<Fine>>()
    val fines: LiveData<List<Fine>> get() = _fines


    fun addEvent(event: Event) {
        eventRepository.save(event)
        val eventsList = eventRepository.findByMonth(MonthUID.create(event.eventDate), false)
        _events.value = eventsList
    }
}