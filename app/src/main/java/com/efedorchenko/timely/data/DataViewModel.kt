package com.efedorchenko.timely.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.efedorchenko.timely.data.repository.DataRepository
import com.efedorchenko.timely.data.repository.RepositoryFactory
import com.efedorchenko.timely.fragment.support.CalendarAdapter
import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.model.DataType.EVENT
import com.efedorchenko.timely.model.DataType.FINE
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.model.MonthUID
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

// TODO: инжектить только фабрику, наследников инициализировать во вторичном конструкторе
class DataViewModel @Inject constructor(
    application: Application,
    private val repositoryFactory: RepositoryFactory,
    private val apiService: ApiService
) : AndroidViewModel(application) {

    private val eventRepository: DataRepository<Event> = repositoryFactory.getRepository(EVENT)
    private val fineRepository: DataRepository<Fine> = repositoryFactory.getRepository(FINE)

    /* Собственные данные */
    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> get() = _events

    private val _fines = MutableLiveData<List<Fine>>()
    val fines: LiveData<List<Fine>> get() = _fines

    private val _monthOffset = MutableLiveData(CalendarAdapter.INITIAL_MONTH_OFFSET)
    val monthOffset: LiveData<Int> get() = _monthOffset


    /* Данные команды */
    private val _membersEvents = MutableLiveData<List<Event>>()
    val memberEvents: LiveData<List<Event>> get() = _membersEvents

    private val _membersFines = MutableLiveData<List<Fine>>()
    val membersFines: LiveData<List<Fine>> get() = _membersFines

    /* Эмит ошибки синзронизации */
    private val _alert = MutableSharedFlow<String>()
    val alert = _alert.asSharedFlow()
    private val emitNotSynced: suspend () -> Unit = {
        _alert.emit(ToastHelper.NOT_SYNCHRONIZED)
    }

    // TODO: Посмотреть, может можно не эмитить, а просто подписаться на events и апдейты будут сами приходить
    /* Эмит необходимости разово обновить данные data */
    private val _needUpdateData = MutableSharedFlow<Boolean>(replay = 0)
    val needUpdateData = _needUpdateData.asSharedFlow()
    suspend fun emitNeedUpdateData() {
        _needUpdateData.emit(true)
    }

    init {
        val monthUID = MonthUID.create()
        _events.value = eventRepository.findByMonth(monthUID, false)
        _fines.value = fineRepository.findByMonth(monthUID, true)
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
    // TODO: использовать просто AbstractData, а не наследника T
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

    fun getEventsAsync(monthOffset: Int, userUuid: String?) = viewModelScope.async {
        val monthUID = MonthUID.create(LocalDate.now().plusMonths(monthOffset.toLong()))
        return@async eventRepository.findByMonth(monthUID, false, userUuid).toEventMap()
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
            if (fineIdForDelete?.let { it1 -> fineRepository.deleteById(it1) } == true) {
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

    fun updateLiveData(position: Int, userUuid: String?) {
        viewModelScope.launch {
            val monthOffset = CalendarAdapter.calculateMonthOffset(position)
            val monthUID = MonthUID.create(LocalDate.now().plusMonths(monthOffset.toLong()))
            doUpdateEventsLiveData(monthUID, userUuid)
            doUpdateFinesLiveData(monthUID, userUuid)
        }
    }

    fun updateLiveData(dataType: DataType, date: LocalDate, userUuid: String? = null) {
        viewModelScope.launch {
            val monthUID = MonthUID.create(date)
            when (dataType) {
                EVENT -> doUpdateEventsLiveData(monthUID, userUuid)
                FINE -> doUpdateFinesLiveData(monthUID, userUuid)
            }
            emitNeedUpdateData()
        }
    }

    private fun doUpdateFinesLiveData(monthUID: MonthUID, userUuid: String? = null) {
        if (userUuid == null) {
            _fines.value = fineRepository.findByMonth(monthUID, true)
        } else {
            _membersFines.value = fineRepository.findByMonth(monthUID, false, userUuid)
        }
    }

    private fun doUpdateEventsLiveData(monthUID: MonthUID, userUuid: String? = null) {
        if (userUuid == null) {
            _events.value = eventRepository.findByMonth(monthUID, false)
        } else {
            _membersEvents.value = eventRepository.findByMonth(monthUID, false, userUuid)
        }
    }

    fun cleanAll() {
        _events.value = emptyList()
        _fines.value = emptyList()

        _membersEvents.value = emptyList()
        _membersFines.value = emptyList()

        eventRepository.clean()
        fineRepository.clean()

        _monthOffset.value = 0
    }
}
