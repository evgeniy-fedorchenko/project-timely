package com.efedorchenko.timely.service

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.efedorchenko.timely.data.Event
import com.efedorchenko.timely.data.Fine
import com.efedorchenko.timely.data.MonthUID
import com.efedorchenko.timely.repository.EventRepository
import com.efedorchenko.timely.repository.FineRepository
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val fineRepository: FineRepository = FineRepository(application)
    private val eventRepository: EventRepository = EventRepository(application)

    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> get() = _events

    private val _fines = MutableLiveData<List<Fine>>()
    val fines: LiveData<List<Fine>> get() = _fines

    init {
        val monthUID = MonthUID.create()
        _events.value = eventRepository.findByMonth(monthUID, false)
        _fines.value = fineRepository.findByMonth(monthUID)
    }

    fun addEvent(event: Event) {
        viewModelScope.launch {
            eventRepository.save(event)
        }
    }

    fun updateSummaryData(monthOffset: Int) {
        val monthUID = MonthUID.create(LocalDate.now().plusMonths(monthOffset.toLong()))
        viewModelScope.launch {
            _events.value = eventRepository.findByMonth(monthUID, false)
        }
        viewModelScope.launch {
            _fines.value = fineRepository.findByMonth(monthUID)
        }
    }

}