package com.efedorchenko.timely.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.efedorchenko.timely.fragment.support.CalendarAdapter
import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.model.DataType.EVENT
import com.efedorchenko.timely.model.DataType.FINE
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.model.MonthUID
import com.efedorchenko.timely.model.SpaceMember
import com.efedorchenko.timely.model.api.ApiResponse
import com.efedorchenko.timely.model.toEventMap
import com.efedorchenko.timely.service.ApiService
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
    private val application: Application,
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

    private val _monthOffset = MutableLiveData(CalendarAdapter.INITIAL_MONTH_OFFSET)
    val monthOffset: LiveData<Int> get() = _monthOffset

    private val _members = MutableLiveData<List<SpaceMember>>()
    val members: LiveData<List<SpaceMember>> get() = _members

    /* Эмит ошибки синзронизации */
    private val _alert = MutableSharedFlow<String>()
    val alert = _alert.asSharedFlow()

    private val emitNotSynced: suspend () -> Unit = {
        _alert.emit(ToastHelper.NOT_SYNCHRONIZED)
    }

    /* Эмит необходимости разово обновить данные data */
    private val _needUpdateData = MutableSharedFlow<Boolean>(replay = 0)
    val needUpdateData = _needUpdateData.asSharedFlow()

    suspend fun emitNeedUpdateData() {
        _needUpdateData.emit(true)
    }

    fun updateMembers() {
        _members.value = memberRepository.getMembersList()
    }

    init {
        val monthUID = MonthUID.create()
        _events.value = eventRepository.findByMonth(monthUID, false)
        _fines.value = fineRepository.findByMonth(monthUID, true)
        _members.value = memberRepository.getMembersList()
    }

    fun addNewData(data: AbstractData) {
        viewModelScope.launch {
            val repository = repositoryFactory.getRepository(data)
            val appId = repository.save(data)
            data.appId = appId
            when (data.getType()) {
                EVENT -> _events.value = (_events.value ?: emptyList()) + data as Event
                FINE -> _fines.value = (_fines.value ?: emptyList()) + data as Fine
            }
            if (!sendData(data)) {
                emitNotSynced.invoke()
            }
        }
    }

    fun deleteData(data: AbstractData) {
        // TODO: not implemented
    }

    fun changeData(data: AbstractData) {
        // TODO: not implemented
    }

    /**
     * При конфликте (на ту же дату отправили другие данные) сервер вернет старые данные -> локальные данные
     * перезапишуться, чтобы юзер не создал данные, которые конфликтуют с теми, что уже сохранены на сервре
     */
    suspend fun <T : AbstractData> sendData(data: T): Boolean {
        var success = false
        try {
            // TODO: если данные с сервера другие - надо обновлять UI
            when (val response = apiService.save(data)) {
                is ApiResponse.Success -> response.data?.let {
                    it.appId = data.appId
                    val repository = repositoryFactory.getRepository(data)
                    repository.upsert(it.toInheritor())
                    success = true
                } ?: run { success = false }

                is ApiResponse.Error -> success = false
            }

        } catch (ex: IOException) {
            success = false
        }
        return success
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

    fun updateLiveData(position: Int) {
        val monthOffset = CalendarAdapter.calculateMonthOffset(position)
        val monthUID = MonthUID.create(LocalDate.now().plusMonths(monthOffset.toLong()))
        doUpdateEventsLiveData(monthUID)
        doUpdateFinesLiveData(monthUID)
    }

    fun updateLiveData(dataType: DataType, date: LocalDate) {
        val monthUID = MonthUID.create(date)
        when (dataType) {
            EVENT -> doUpdateEventsLiveData(monthUID)
            FINE -> doUpdateFinesLiveData(monthUID)
        }
    }

    private fun doUpdateFinesLiveData(monthUID: MonthUID) {
        viewModelScope.launch {
            _fines.value = fineRepository.findByMonth(monthUID, true)
        }
    }

    private fun doUpdateEventsLiveData(monthUID: MonthUID) {
        viewModelScope.launch {
            _events.value = eventRepository.findByMonth(monthUID, false)
        }
    }

    fun cleanAll() {
        _events.value = emptyList()
        _fines.value = emptyList()
        _members.value = emptyList()

        eventRepository.clean()
        fineRepository.clean()
        memberRepository.clean()

        _monthOffset.value = 0
    }

}
