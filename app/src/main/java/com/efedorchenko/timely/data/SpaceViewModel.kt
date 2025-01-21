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

    fun clean() {
        viewModelScope.launch {
            _members.value = emptyList()
            memberRepository.clean()
        }
    }

    /**
     * Переключать юзера можно только когда его данные уже загружены в соответсвующую viewModel
     */
    fun switchTo(spaceMember: SpaceMember) {
        _selectedMember.value = spaceMember
    }

    /**
     * Переключать юзера можно только когда его данные уже загружены в соответсвующую viewModel
     */
    fun resetSelectedMember() {
        _selectedMember.value = null
    }
}
