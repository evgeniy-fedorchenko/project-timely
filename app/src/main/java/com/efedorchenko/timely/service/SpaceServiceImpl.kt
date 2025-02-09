package com.efedorchenko.timely.service

import android.util.Log
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.SpaceViewModel
import com.efedorchenko.timely.data.repository.MemberRepository
import com.efedorchenko.timely.model.MembersResult
import com.efedorchenko.timely.model.SyncProcess.UpdateResult
import com.efedorchenko.timely.model.api.ApiResponse
import com.efedorchenko.timely.model.auth.RoleType
import javax.inject.Inject

class SpaceServiceImpl @Inject constructor(
    private val apiService: ApiService,
    private val memberRepository: MemberRepository,
    private val encProfileStorage: EncProfileStorage,
    private val spaceViewModel: SpaceViewModel
) : SpaceService {

    override suspend fun initMembers() =
        doUpdateMembers { apiService.getMembers() } == UpdateResult.SUCCESS

    override suspend fun updateMembers() =
        doUpdateMembers { apiService.getMembers(memberRepository.getMaxChangedAt()) }


    // TODO: принимать параметр userUuid: String? - при отсутствии - кикать currentUser, при наличии - кикать переданного
    override suspend fun leaveSpace(): Boolean {
        when (val response = apiService.leaveSpace()) {
            is ApiResponse.Success -> return response.data ?: false
            is ApiResponse.Error -> {
                Log.e("Network error",
                    "Cannot request for leave space for user: ${encProfileStorage.getUserUuid()}." +
                        "ApiErrorCode: ${response.apiErrorCode}, " +
                        "error message: ${response.errorMessage}, " +
                        "error data: ${response.errorData}"
                )
                return false
            }
        }
    }

    private suspend fun doUpdateMembers(requestFunc: suspend () -> ApiResponse<MembersResult>): UpdateResult {
        when (val response = requestFunc.invoke()) {
            is ApiResponse.Success -> {
                response.data?.let {
                    if (!it.youConsistInSpace) {
                        return UpdateResult.NOT_CONSIST_IN_SPACE
                    }
                    if (it.members.isNotEmpty()) {

                        /* Работники отображаются у всех (в тч друг у друга). Руководители только у создателя,
                         * а создатель ни у кого. При этом сам работник у себя не отображается */
                        val userUuid = encProfileStorage.getUserUuid()
                        it.members.removeIf { member -> member.userUuid == userUuid || member.role == RoleType.CREATOR }
                        if (encProfileStorage.getRole() != RoleType.CREATOR) {
                            it.members.removeIf { member -> member.role == RoleType.BOSS }
                        }
                        memberRepository.save(it.members)
                        memberRepository.deleteIfNotContains(it.actualIds)
                        spaceViewModel.updateMembers()
                    }
                    return UpdateResult.SUCCESS
                }
                return UpdateResult.FAIL
            }
            is ApiResponse.Error -> {
                Log.e("Network error", "Cannot get members from server." +
                        "ApiErrorCode: ${response.apiErrorCode}, " +
                        "error message: ${response.errorMessage}, " +
                        "error data: ${response.errorData}"
                )
                return UpdateResult.FAIL
            }
        }
    }
}

/*
1. Разделение хранилищ
- Вместо очистки всей БД и перезаписи данных, можно создать отдельные таблицы
- Одна таблица для личных смен пользователя (они всегда там)
- Вторая таблица для временного хранения просматриваемых смен других сотрудников
- При переключении между сотрудниками работаем только со второй таблицей
- Это избавит от необходимости постоянно перезагружать личные смены

3. Пагинация данных
- Не обязательно сразу загружать все смены за большой период
- Можно подгружать данные постепенно при прокрутке календаря
- Например, сначала загрузить текущий месяц
- При прокрутке подгружать следующие месяцы
- Это ускорит первоначальную загрузку

4. Предварительная загрузка
- Когда пользователь открывает список сотрудников, можно начать загрузку их смен заранее
- Пока пользователь выбирает сотрудника, данные уже будут загружаться
- К моменту выбора часть данных может быть уже готова
- Это сократит время ожидания

5. Улучшение UX при загрузке
- Показывать промежуточное состояние загрузки
- Можно отображать календарь сразу, просто без данных
- Постепенно заполнять его по мере загрузки смен
- Показывать прогресс загрузки
- Это создаст ощущение более быстрой работы приложения

7. Умное обновление данных
- Не обновлять данные, если они не изменились
- Использовать временные метки последнего обновления
- Синхронизировать только изменившиеся данные
- Это уменьшит количество необходимых загрузок

8. Фоновая синхронизация
- Периодически обновлять данные в фоне
- Загружать данные заранее для часто просматриваемых сотрудников
- Это обеспечит актуальность данных без задержек при просмотре
*/
