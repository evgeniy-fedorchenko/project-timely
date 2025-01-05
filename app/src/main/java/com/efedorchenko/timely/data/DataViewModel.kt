package com.efedorchenko.timely.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.DataType.EVENT
import com.efedorchenko.timely.model.DataType.FINE
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.model.MonthUID
import com.efedorchenko.timely.model.SpaceMember
import com.efedorchenko.timely.model.api.ApiResponse
import com.efedorchenko.timely.model.toEventMap
import com.efedorchenko.timely.service.ApiService
import com.efedorchenko.timely.service.CalendarAdapter
import com.efedorchenko.timely.service.ToastHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okio.IOException
import org.threeten.bp.LocalDate
import javax.inject.Inject

// TODO: Когда юзер логинится - просить все ивенты с бека и обновлять бд

class DataViewModel @Inject constructor(
    application: Application,
    private val eventRepository: DataRepository<Event>,
    private val fineRepository: DataRepository<Fine>,
    private val repositoryFactory: RepositoryFactory,
    private val memberRepository: MemberRepository,
    private val apiService: ApiService
) : AndroidViewModel(application) {

    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> get() = _events

    private val _fines = MutableLiveData<List<Fine>>()
    val fines: LiveData<List<Fine>> get() = _fines

    private val _monthOffset = MutableLiveData<Int>()
    val monthOffset: LiveData<Int> get() = _monthOffset

    private val _members = MutableLiveData<List<SpaceMember>>()
    val members: LiveData<List<SpaceMember>> get() = _members

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
        _members.value = memberRepository.getMembersList()
    }

    fun addData(data: AbstractData) {
        viewModelScope.launch {
            val repository = repositoryFactory.getRepository(data)
            val appId = repository.save(data)
            data.appId = appId
            when (data.getType()) {
                EVENT ->  _events.value = (_events.value ?: emptyList()) + data as Event
                FINE -> _fines.value = (_fines.value ?: emptyList()) + data as Fine
            }
            if (!sendData(data)) {
                emitError.invoke()
            }
        }
    }

    fun deleteData(data: AbstractData) {
        // TODO: not implemented
    }

    fun changeData(data: AbstractData) {
        // TODO: not implemented
    }

    suspend fun <T : AbstractData> sendData(data: T): Boolean {
        var success = false
        try {

            when (val dataFromServer = apiService.save(data)) {
                is ApiResponse.Success -> dataFromServer.data?.let {
                    it.appId = data.appId
                    val repository = repositoryFactory.getRepository(data)
                    repository.setBackendId(it.toInheritor())
                    success = true
                } ?: run { success = false }

                is ApiResponse.Error -> success = false
            }

        } catch (ex: IOException) {
            success = false
        }
        return success
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

    // FIXME: заменить на deleteData(data: AbstractData)
    fun delete(position: Int) {
        val currentList = _fines.value?.toMutableList() ?: return

        _fines.value?.let {
            val fineIdForDelete = it[position].appId
            if (fineRepository.deleteById(fineIdForDelete)) {
                currentList.removeAt(position)
                _fines.value = currentList
            }
        }
    }

    fun getNotSyncedEvents(): List<Event> {
        return eventRepository.findNullableBackendId()
    }

    fun getNotSyncedFine(): List<Fine> {
        return fineRepository.findNullableBackendId()
    }
}
