package com.efedorchenko.timely.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.efedorchenko.timely.data.repository.MemberRepository
import com.efedorchenko.timely.model.member.SpaceMember
import com.efedorchenko.timely.model.member.SpaceStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/*
* StateFlow  - хранение состояния, для многих подпиcчиков, хранит значение вплоть до его изменения (замена LiveData)
* SharedFlow - единоразовая передача события подписчику (или нескольким с reply)
*/

/**
 * Как происходит процесс переключения юзера:
 *
 * Когда юзер выбирает участника в списке, вызывается `SpaceDialogFragment.showMember(member: SpaceMember)`.
 * Загружаются его данные с локальной БД и дельта обновлений по API. Особенности
 *
 * #### Загрузка данных и момент переключения View
 *
 * Первым делом выполняется проверка: присутствуют ли хоть какие-то данные по этому юзеру в локальном репозитории
 *   - **Если данных нет:** происходит синхронный запрос к API за данными. При успехе, данные всех
 *     типов (если они есть в ответе, мб у юзера нет данных) загружаются в в соответсвующую viewModel
 *     и таблицу в репозитории, после чего происходит принудительное обновление UI через
 *     `emitNeedUpdateEvents` и `emitNeedUpdateFine`, опять же, только если данные в ответе были.
 *     Только после этого происходит переключение на выбранного юзера.
 *     Если запрос провалился, то выводится тост и переключения на юзера не происходит
 *
 *   - **Если данные есть:** переключение на юзера происходит немедленно, отрисовывая найденные данные. Асинхронно
 *     выполняется запрос к API за обновлениями. Если запрос провалился, выводится тост `viewModel.emitNotSynced`,
 *     иначе новые данные (если они есть в ответе) сохраняются в репозиторий и viewModel и происходит принудительное
 *     обновление UI (опять же если они есть в ответе), отрисовывая новые данные
 *
 * #### Процесс переключения View
 * Если данные были загружены (или просто нашлись в локальном репозитории) выполняется переключение на юзера через
 * `spaceViewModel.switchTo(member)`. Это [StateFlow], которая начинает транслировать участника, на которого нужно
 * переключиться. Подписчики ловят юзера и обновляют свои компоненты UI. Например пересоздается
 * viewPager, чтобы привязаться к новому юзеру и управлять им. Календарь и прочие фрагменты переключенного
 * юзера отрисовываются теми же средствами, что и фрагменты текущего юзера.
 * При выполнении действий на экране компоненты проверяют наличие переключенного юзера через обращение к этому
 * [StateFlow], чтобы понять, имеет ли текущий авторизованный юзер права на выполнения действий.
 * Например: добавление новых смен доступно для "себя", но не доступно при просмотре переключенного юзера.
 * А так как фрагмент один и тот же - [members] выступает как общий флаг хранения этого состояния.
 *
 * #### FAQ
 * Зачем нужно несколько обновлений UI?
 * - Первое обновление отображает только локальные данные по этому юзеру. А обновления загружаются асинхронно,
 *   после их получения нужно снова обновить UI, чтобы их отобразить. Таким образом текущий юзер вынужден ждать
 *   загрузки только при первом переключении, все последующие загрузки выполняются в фоне и по завершении вносят
 *   свои новые данные на UI
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
    private val _members = MutableStateFlow(memberRepository.getMembersList(SpaceStatus.MEMBER))
    val members: StateFlow<List<SpaceMember>> get() = _members

    /** Юзер из команды, выбранный для просмотра. NULL означает текущего авторизованного юзера */
    private val _selectedMember = MutableStateFlow<SpaceMember?>(null)
    val selectedMember: StateFlow<SpaceMember?> get() = _selectedMember

    /**
     * Эмит необходимости переключить пункты навигационное меню между двумя состояниями:
     * "Юзер, состоящий в пространстве" и "Юзер, не состоящий в пространстве"иня
     * - Юзер вступил в пространство -> показать кнопки пространства
     * - юзер вышел из пространства -> убрать эти кнопки и показать кнопку вступления
     */
    private val _needSwitchSideMenuItems = MutableSharedFlow<Boolean>()
    val needSwitchSideMenuItems = _needSwitchSideMenuItems.asSharedFlow()
    fun needSwitchSideMenuItems() {
        viewModelScope.launch { _needSwitchSideMenuItems.emit(true) }
    }

    /**
     * Уведомление о том, что статус работника в пространстве был изменен. Новый статус содержится в этом эмите.
     * Перед началом эмита все изменения, связанные со сменой статуса (например сохранение `spaceName`)
     * уже должны быть произведены
     *
     * @see SpaceStatus
     */
    private val _statusChangedNty = MutableSharedFlow<SpaceStatus>()
    val statusChangedNty = _statusChangedNty.asSharedFlow()
    fun emitStatusChanged(newStatus: SpaceStatus) {  // Если передается MEMBER, то spaceName должно быть уже сохранено
        viewModelScope.launch { _statusChangedNty.emit(newStatus) }
    }

    fun updateMembers() {
        _members.value = memberRepository.getMembersList(SpaceStatus.MEMBER)
    }

    fun getJoinRequests(): List<SpaceMember> {
        return memberRepository.getMembersList(SpaceStatus.PENDING_WORKER, SpaceStatus.PENDING_BOSS)
    }

    /**
     * Принять участника в пространство как работника.
     * Метод обновляет запись об участнике в БД, устанавливая ему статус [SpaceStatus.MEMBER],
     * а так же добавляет обновленного участника в [StateFlow] учасников - [_members]
     */
    fun acceptMember(member: SpaceMember) {
        memberRepository.updateStatus(SpaceStatus.MEMBER, member.userUuid)
        member.spaceStatus = SpaceStatus.MEMBER
        _members.value = (_members.value + member)
    }

    /**
     * Удалить запись об участнике из репозитория и из [StateFlow] списка учасников - [_members]
     */
    fun removeMember(member: SpaceMember) {
        memberRepository.delete(member.userUuid)
        val mutableMembers = _members.value.toMutableList()
        if (mutableMembers.removeIf { it.userUuid == member.userUuid }) {
            _members.value = mutableMembers
        }
    }

    /**
     * Отреагировать на факт того, что бзер был отсоединен от своего пространства.
     * При вызове произойдут следующие действия:
     * - Очистка [MemberRepository]
     * - Очистка [members], местный `StateFlow<List<SpaceMember>>`
     * - Сброс просматриваемого юзера [selectedMember] (если был установлен)
     * - Отправка эмита о смене статуса на [SpaceStatus.NONE]
     * - Отправка эмита о необходимости переключить пункты бокового навигационного меню
     */
    fun detachFromSpace() {
        viewModelScope.launch {
            cleanAll()
            emitStatusChanged(SpaceStatus.NONE)
            needSwitchSideMenuItems()
        }
    }

    /**
     * Очистить хранилище: [MemberRepository] и [members], местный `StateFlow<List<SpaceMember>>`
     */
    fun cleanAll() {
        viewModelScope.launch {
            _members.value = emptyList()
            memberRepository.clean()
            if (_selectedMember.value != null) resetSelectedMember()
        }
    }

    /**
     * Переключиться на юзера из команды - отображение, если роль позволяет - редактирование
     */
    fun switchTo(spaceMember: SpaceMember) {
        _selectedMember.value = spaceMember
    }

    /**
     * Переключиться на авторизованного юзера - "вернуться домой"
     */
    fun resetSelectedMember() {
        _selectedMember.value = null
    }
}
