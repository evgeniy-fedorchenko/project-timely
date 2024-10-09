package com.efedorchenko.timely.service

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.model.MonthUID
import com.efedorchenko.timely.model.toEventMap
import com.efedorchenko.timely.repository.EventRepository
import com.efedorchenko.timely.repository.FineRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate

// TODO: Когда юзер логинится - просить все ивенты с бека и обновлять бд

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val eventsCache: MutableMap<MonthUID, MutableMap<LocalDate, Event>> = HashMap()
    }

    private val eventRepository: EventRepository = EventRepository(application)
    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> get() = _events

    private val fineRepository: FineRepository = FineRepository(application)
    private val _fines = MutableLiveData<List<Fine>>()
    val fines: LiveData<List<Fine>> get() = _fines

    private val _monthOffset = MutableLiveData<Int>()
    val monthOffset: LiveData<Int> get() = _monthOffset

    init {
        val monthUID = MonthUID.create()
        _events.value = eventRepository.findByMonth(monthUID, false)
        _fines.value = fineRepository.findByMonth(monthUID)
        _monthOffset.value = CalendarPageAdapter.INITIAL_MONTH_OFFSET
        viewModelScope.launch { }   // Инициализация CoroutineContext
    }

    fun addEvent(event: Event) {
        _events.value = (_events.value ?: emptyList()) + event
        viewModelScope.launch {
            eventRepository.save(event)
            var monthEvents = eventsCache[MonthUID.create(event.eventDate)]
            monthEvents?.let { monthEvents[event.eventDate] = event }
        }
    }

    fun updateSummaryData(position: Int) {
        val monthOffset = CalendarPageAdapter.calculateMonthOffset(position)
        val monthUID = MonthUID.create(LocalDate.now().plusMonths(monthOffset.toLong()))
        viewModelScope.launch {
            _events.value = eventRepository.findByMonth(monthUID, false)
        }
        viewModelScope.launch {
            _fines.value = fineRepository.findByMonth(monthUID)
        }
    }

    fun getEventsAsync(monthOffset: Int) = viewModelScope.async {
        val monthUID = MonthUID.create(LocalDate.now().plusMonths(monthOffset.toLong()))
        var monthEvents = eventsCache[monthUID]

        if (monthEvents == null) {
            monthEvents = eventRepository.findByMonth(monthUID, false).toEventMap()
            eventsCache[monthUID] = monthEvents
        }
        monthEvents
    }

    fun updateMonthOffset(position: Int) {
        val monthOffset = CalendarPageAdapter.calculateMonthOffset(position)
        _monthOffset.value = monthOffset
    }
}