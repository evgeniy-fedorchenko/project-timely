package com.efedorchenko.timely.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.model.MonthUID
import com.efedorchenko.timely.model.api.ApiResponse
import com.efedorchenko.timely.model.toEventMap
import com.efedorchenko.timely.service.ApiService
import com.efedorchenko.timely.service.CalendarAdapter
import com.efedorchenko.timely.service.ToastHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okio.IOException
import org.threeten.bp.LocalDate
import javax.inject.Inject

// TODO: Когда юзер логинится - просить все ивенты с бека и обновлять бд

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val eventRepository: DataRepository<Event>,
    private val fineRepository: DataRepository<Fine>,
    private val apiService: ApiService
) : AndroidViewModel(application) {

    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> get() = _events

    private val _fines = MutableLiveData<List<Fine>>()
    val fines: LiveData<List<Fine>> get() = _fines

    private val _monthOffset = MutableLiveData<Int>()
    val monthOffset: LiveData<Int> get() = _monthOffset

    private val _alert = MutableSharedFlow<String>()
    val alert = _alert.asSharedFlow()

    private val emitError: suspend () -> Unit = {
        _alert.emit(ToastHelper.NOT_SYNCHRONIZED)
    }

    init {
        val monthUID = MonthUID.create()
        _events.value = eventRepository.findByMonth(monthUID, false)
        _fines.value = fineRepository.findByMonth(monthUID, true)
        _monthOffset.value = CalendarAdapter.INITIAL_MONTH_OFFSET
        viewModelScope.launch { }   // Инициализация CoroutineContext
    }

    fun addEvent(event: Event) {
        viewModelScope.launch {
            val localDdId = eventRepository.save(event)
            event.appId = localDdId
            _events.value = (_events.value ?: emptyList()) + event

            sendEvent(event)
        }
    }

    suspend fun sendEvent(event: Event) {
        try {
            when (val eventFromServer = apiService.save(event)) {
                is ApiResponse.Success -> eventFromServer.data?.let {
                    it.appId = event.appId
                    eventRepository.setBackendId(it)
                } ?: run {
                    emitError.invoke()
                }

                is ApiResponse.Error -> emitError.invoke()
            }
        } catch (ex: IOException) {
            emitError.invoke()
        }
    }

    fun deleteEvent(event: Event) {
        // TODO: not implemented
    }

    fun changeEvent(event: Event) {
        // TODO: not implemented
    }

    fun addFine(fine: Fine) {
        viewModelScope.launch {
            val id = fineRepository.save(fine)
            if (id > 0) {
                fine.id = id
                _fines.value = (_fines.value ?: emptyList()) + fine
            }
        }
    }

    fun deleteFine(fine: Fine) {
        // TODO: not implemented
    }

    fun changeFine(fine: Fine) {
        // TODO: not implemented
    }

    fun updateSummaryData(position: Int) {
        val monthOffset = CalendarAdapter.calculateMonthOffset(position)
        val monthUID = MonthUID.create(LocalDate.now().plusMonths(monthOffset.toLong()))
        viewModelScope.launch {
            _events.value = eventRepository.findByMonth(monthUID, false)
        }
        viewModelScope.launch {
            _fines.value = fineRepository.findByMonth(monthUID, true)
        }
    }

    fun getEventsAsync(monthOffset: Int) = viewModelScope.async {
        val monthUID = MonthUID.create(LocalDate.now().plusMonths(monthOffset.toLong()))
        return@async eventRepository.findByMonth(monthUID, false).toEventMap()
    }

    fun updateMonthOffset(position: Int) {
        val monthOffset = CalendarAdapter.calculateMonthOffset(position)
        _monthOffset.value = monthOffset
    }

    fun delete(position: Int) {
        val currentList = _fines.value?.toMutableList() ?: return

        _fines.value?.let {
            val fineIdForDelete = it[position].id
            if (fineRepository.deleteById(fineIdForDelete)) {
                currentList.removeAt(position)
                _fines.value = currentList
            }
        }
    }

    fun getEventsOutOfSync(): List<Event> {
        return eventRepository.findNullableBackendId()
    }

    fun getFinesOutOfSync(): List<Fine> {
        return fineRepository.findNullableBackendId()
    }
}
