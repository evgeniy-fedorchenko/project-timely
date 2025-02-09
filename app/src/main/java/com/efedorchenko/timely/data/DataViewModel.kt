package com.efedorchenko.timely.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.efedorchenko.timely.data.repository.DataRepository
import com.efedorchenko.timely.data.repository.RepositoryFactory
import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.model.DataType.EVENT
import com.efedorchenko.timely.model.DataType.FINE
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.model.MonthUID
import com.efedorchenko.timely.model.calendar.toEventMap
import com.efedorchenko.timely.service.ToastHelper
import com.efedorchenko.timely.ui.support.CalendarAdapter
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import javax.inject.Inject

/*
* Как сохраняются и отображаются данные в разных сценариях:
*
* 1. При создании нового инстанса фрагмента каледаря, в самой пеовой фазе (в onCreate) вызывается метод getEventsAsync,
*    чтобы данные загружались из репозитория параллельно с построением фрагмента. Далее (как можно позде) происходит
*    проверка готовности и ожидание, если данные еще не готовы. После получения данных они отрисовываются на каледаре
*    Почему не используется прямое взятие из viewModel.events?
*    - Потому что там содержаться данные текущего месяца, а не того, который требуется построить
*    Почему не выполняется updateLiveData, чтобы загрузить во viewModel.events данные, а потом просто не взять их?
*    - Потому что нет гарантий, что к моменту взятия данные там будут лежать уже новые готовые данные. Есть вероятность,
*      что поле не успеет обновиться и будут взяты новые данные
*
* 2. При добавлении нового события руками юзера в календарь
*    Выполнение исходит из AddEventDialog или AddFineDialog, который принимает на вход слушатель кнопки сохранения,
*    который реализован прямо на соответствующем фрагменте. Соотвтетствеено при сохранении выполнение переходит во
*    фрагмент, где происходит обновление ячейки (CalendarFragment.updateCell), данные смены рисуются на соответсвующей
*    ячейке. Так же параллельно viewModel сохраняет и отправляет данные в фоне
*
* 3. При получении новых данных по http (через обновление данных). Сервис, занимающийся вызовом API сохраняет полученные
*    данные в репозиторий, а так же вызывает метод updateLiveData, который перезагружает viewModel новыми, только что
*    сохраненными данными и емитит фгал необходимости обновления данных. Фрагмент ловит этот флаг и перерисовывает весь
*    календарь
*    Почему бы просто не подписаться на обновления viewModel.events?
*    - Потому что это помешает четкому полению данных при изначальном создании календаря. Когда надо иметь контроль
*      над моментом запуска обновления и взятия новых данных. Если запустить обновление во фрагменте как можно раньше
*      и далее просто налеяться на этого слушателя - не получиться выставить на него таймаут получения данных и
*      отобразить ошибку загрузки.
*/
class DataViewModel @Inject constructor(
    application: Application,
    private val repositoryFactory: RepositoryFactory,
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
    val emitNotSynced: suspend () -> Unit = {
        _alert.emit(ToastHelper.NOT_SYNCED)
    }

    // TODO: Посмотреть, может можно не эмитить, а просто подписаться на events и апдейты будут сами приходить
    /* Эмит необходимости разово обновить данные data */
    private val _needUpdateData = MutableSharedFlow<Boolean>(replay = 0)
    val needUpdateData = _needUpdateData.asSharedFlow()
    val emitNeedUpdateData: suspend () -> Unit = {
        _needUpdateData.emit(true)
    }

    init {
        val monthUID = MonthUID.create()
        _events.value = eventRepository.findByMonth(monthUID, false)
        _fines.value = fineRepository.findByMonth(monthUID, true)
    }

    @Suppress("unchecked_cast")
    fun <T : AbstractData> get(dataType: DataType, userUuid: String? = null): List<T>? {
        return when (dataType) {
            EVENT -> if (userUuid == null) events.value as? List<T> else memberEvents.value as? List<T>
            FINE -> if (userUuid == null) fines.value as? List<T> else membersFines.value as? List<T>
        }
    }

    fun add(data: AbstractData, userUuid: String?) {
        when (data.getType()) {
            EVENT -> {
                if (userUuid == null) {
                    when {
                        _events.value.isNullOrEmpty() -> _events.value = mutableListOf(data as Event)
                        data.appId != null -> _events.value =
                            eventRepository.findByMonth(MonthUID.create(data.date), true)
                        else -> _events.value = _events.value!! + data as Event
                    }
                } else {
                    when {
                        _membersEvents.value.isNullOrEmpty() -> _membersEvents.value = mutableListOf(data as Event)
                        data.appId != null -> _membersEvents.value =
                            eventRepository.findByMonth(MonthUID.create(data.date), true, userUuid)
                        else -> _membersEvents.value = _membersEvents.value!! + data as Event
                    }
                }
            }
            FINE -> {
                userUuid?.let { _membersFines.value = (_membersFines.value ?: emptyList()) + data as Fine }
                    ?: run { _fines.value = (_fines.value ?: emptyList()) + data as Fine }
            }
        }
    }

    fun getEventsAsyncStart(monthOffset: Int, userUuid: String?) = viewModelScope.async {
        val monthUID = MonthUID.create(LocalDate.now().plusMonths(monthOffset.toLong()))
        return@async eventRepository.findByMonth(monthUID, true, userUuid).toEventMap()
    }

    fun updateMonthOffset(position: Int) {
        val monthOffset = CalendarAdapter.calculateMonthOffset(position)
        _monthOffset.value = monthOffset
    }

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

    fun getNotSynced(dataType: DataType): List<AbstractData> {
        return repositoryFactory.get(dataType).findNullableBackendId()
    }

    fun updateLiveData(position: Int, userUuid: String?) {
        viewModelScope.launch {
            val monthOffset = CalendarAdapter.calculateMonthOffset(position)
            val monthUID = MonthUID.create(LocalDate.now().plusMonths(monthOffset.toLong()))
            doUpdateEventsLiveData(monthUID, userUuid)
            doUpdateFinesLiveData(monthUID, userUuid)
        }
    }

    fun updateLiveData(dataType: DataType? = null, userUuid: String? = null) {
        viewModelScope.launch {
            val yearMonth = YearMonth.now().plusMonths(monthOffset.value?.toLong() ?: 0)
            val monthUID = MonthUID.create(yearMonth)
            dataType?.let {
                when (dataType) {
                    EVENT -> doUpdateEventsLiveData(monthUID, userUuid)
                    FINE -> doUpdateFinesLiveData(monthUID, userUuid)
                }
            } ?: run {
                doUpdateEventsLiveData(monthUID, userUuid)
                doUpdateFinesLiveData(monthUID, userUuid)
            }
        }
    }

    private fun doUpdateFinesLiveData(monthUID: MonthUID, userUuid: String? = null) {
        if (userUuid == null) {
            _fines.value = fineRepository.findByMonth(monthUID, true)
        } else {
            _membersFines.value = fineRepository.findByMonth(monthUID, false, userUuid)
        }
    }

    private suspend fun doUpdateEventsLiveData(monthUID: MonthUID, userUuid: String? = null) {
        if (userUuid == null) {
            _events.value = eventRepository.findByMonth(monthUID, false)
        } else {
            _membersEvents.value = eventRepository.findByMonth(monthUID, false, userUuid)
        }
        emitNeedUpdateData.invoke()
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
