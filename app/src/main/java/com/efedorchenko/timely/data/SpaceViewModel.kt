package com.efedorchenko.timely.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.efedorchenko.timely.data.repository.MemberRepository
import com.efedorchenko.timely.model.SpaceMember
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/*
* StateFlow  - хранение состояния, для многих подпиcчиков, хранит значение до его изменения (замена LiveData)
* SharedFlow - единоразовая передача события подписчику (или нескольким с reply)
*/

/**
 * Как происходит процесс переключения юзера:
 *
 * Когда юзер выбирает участника в списке, вызывается `SpaceDialogFragment.showMember(member: SpaceMember)`.
 * Загружаются его данные в таком порядке:
 *
 * 1. Проверка есть ли хоть какие-то данные по этому юзеру в локальном репозитории
 *     - Если нет, то происходит запрос к API за данными. При успехе, данные всех типов
 *       (если они есть в ответе, мб у юзера нет данных) загружаются в в соответсвующую viewModel и таблицу
 *       в репозитории, после чего происходит принудительное обновление UI через
 *       `emitNeedUpdateEvents` и `emitNeedUpdateFine`, опять же, только если данные в ответе были
 *       Если запрос провалился, то выводится тост и переключения на юзера не происходит
 *
 *     - Если да, то сразу происходит переключение юзера через `spaceViewModel.switchTo(member)`, и асинхронно
 *       выполняется запрос к API за обновлениями. Если запрос провалился, выводится тост `viewModel.emitNotSynced`,
 *       иначе новые данные (если они есть в ответе) сохраняются в репозиторий и viewModel и происходит принудительное
 *       обновление UI (опять же если они есть в ответе).
 *
 * 2. Если данные были загружены (или просто нашлись в тольком репозитории) выполняется переключение на юзера через
 *    spaceViewModel.switchTo(member)`. Подписчики ловят юзера и обновляют свои компоненты UI. Например пересоздается
 *    viewPager, чтобы привязаться к новому юзеру.
 *
 * Зачем нужно несколько обновлений UI?
 * - Первое обновление отображает только локальные данные по этому юзеру. А обновления загружаются асинхронно,
 *   после их получения нужно снова обновить UI, чтобы их отобразить
 *
 * Обновление данных по API отличается от загрузки данных по API, тем что при обновлении происходит запрос с указанием
 * дельты изменений и возвращаются только данные вне этой дельты. Во время загрузки данных запрашивается просто большой
 * промежуток данных.
 *
 * Кофликты:
 * - В случае, если по API пришли данные, которые уже сохранены локально, но отличаются по идентификатору, правда
 *   остается за пришедшими данными, как база данных севрера - источник правды и клиенты отталкиваются от нее.
 *   Репозиторий выполняет `upsert(ON_CONFLICT_REPLACE)` по уникальным полям каждой сущности
 */
class SpaceViewModel @Inject constructor(
    application: Application,
    private val memberRepository: MemberRepository,
) : AndroidViewModel(application) {

    /** Список участников команды */
    private val _members = MutableStateFlow(memberRepository.getMembersList())
    val members: StateFlow<List<SpaceMember>> get() = _members

    /** Юзер из команды, выбранный для просмотра. NULL означает текущего авторизованного юзера */
    private val _selectedMember = MutableStateFlow<SpaceMember?>(null)
    val selectedMember: StateFlow<SpaceMember?> get() = _selectedMember

    /**
     * Эмит необходимости переключить навигационное меню.
     * Юзер вступил в пространство -> показать кнопки пространства,
     * юзер вышел из пространства -> убрать эти кнопки и показать кнопку вступления
     */
    private val _needSwitchSpaceItemsInSideMenu = MutableSharedFlow<Boolean>()
    val needSwitchSpaceItemsInSideMenu = _needSwitchSpaceItemsInSideMenu.asSharedFlow()
    suspend fun needSwitchSpaceItemsInSideMenu() {
        _needSwitchSpaceItemsInSideMenu.emit(true)
    }

    fun updateMembers() {
        _members.value = memberRepository.getMembersList()
    }

    fun cleanAll() {
        viewModelScope.launch {
            _members.value = emptyList()
            memberRepository.clean()
            resetSelectedMember()
        }
    }

    /**
     * Переключиться на юзера из команды - отображение, если роль позволяет - редактирование.
     */
    fun switchTo(spaceMember: SpaceMember) {
        _selectedMember.value = spaceMember
    }

    /**
     * Переключиться на авторизованного юзера - "вернуться домой".
     */
    fun resetSelectedMember() {
        _selectedMember.value = null
    }
}
