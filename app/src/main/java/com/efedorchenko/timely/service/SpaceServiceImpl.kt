package com.efedorchenko.timely.service

import android.util.Log
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.MemberRepository
import com.efedorchenko.timely.data.RepositoryFactory
import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.DataRangeRequest
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.model.MembersResult
import com.efedorchenko.timely.model.SpaceMember
import com.efedorchenko.timely.model.api.ApiResponse
import com.efedorchenko.timely.model.auth.RoleType
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import javax.inject.Inject

class SpaceServiceImpl @Inject constructor(
    private val apiService: ApiService,
    private val memberRepository: MemberRepository,
    private val repositoryFactory: RepositoryFactory,
    private val encProfileStorage: EncProfileStorage,
    private val viewModel: DataViewModel
) : SpaceService {

    override suspend fun initMembers(): Boolean {
        return doUpdateMembers { apiService.getMembers() } == UpdateResult.SUCCESS
    }

    override fun downloadMember(member: SpaceMember) {
        TODO("Not yet implemented")
    }

    override suspend fun initData(): InitResult {
        val userUuid = encProfileStorage.getUserUuid() ?: return InitResult.ENC_PROFILE_NULL

        val startInclusive = YearMonth.now().minusMonths(2L)
        val endInclusive = YearMonth.now().plusMonths(1L)
        val requestBody = DataRangeRequest(startInclusive, endInclusive, userUuid)

        if (!doUpdateData(DataType.EVENT) { apiService.getRange(requestBody, DataType.EVENT) }) {
            return InitResult.FILED
        }
        if (!doUpdateData(DataType.FINE) { apiService.getRange(requestBody, DataType.FINE) }) {
            return InitResult.FINES_FILED
        }
        return InitResult.SUCCESS
    }

    /**
     * Запросить новые данные с сервера
     *
     * Отправляется наивысший `changed_at`, в ответе приходят все события после него - все они новые.
     * Полученные данные сохраняются и обновляются их `LiveData`.
     * По очереди для каждого типа данных: `Event`, `Fine`, `SpaceMember`
     */
    override suspend fun updateData(userId: String?, withMembers: Boolean): UpdateResult {
        DataType.entries.forEach { dataType ->
            val repository = repositoryFactory.getRepository<AbstractData>(dataType)
            val since = repository.getMaxChangedAt()
            if (!doUpdateData(dataType) { apiService.getUpdates(userId, dataType, since) }) {
                return@updateData UpdateResult.FAIL
            }
        }
        if (!withMembers) {
            return UpdateResult.SUCCESS
        }
        return doUpdateMembers { apiService.getMembers(memberRepository.getMaxChangedAt()) }
    }

    private suspend fun doUpdateData(type: DataType, func: suspend () -> ApiResponse<List<AbstractData>>): Boolean {
        when (val response = func.invoke()) {
            is ApiResponse.Success -> {
                response.data?.let {
                    if (it.isNotEmpty()) {
                        val repository = repositoryFactory.getRepository(it[0])
                        repository.upsertBatch(it)
                        viewModel.updateLiveData(type, LocalDate.now())
                        viewModel.emitNeedUpdateData()
                    }
                    return true
                }
                return false
            }
            is ApiResponse.Error -> {
                Log.e("Network error", "Cannot request data of $type." +
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

                        /* Для каждой роли отображаются только юзеры той же самой или более низкой роли:
                         * - Для работников - только работники, при этом сам работник отображается у всех
                         * - Для руководителей - работники и руководители, при этом сами руководители отображаются
                         *       только у других руководителей и создателя
                         * - Для создателя - работники и руководители, а сам создатель не отображатеся ни у кого
                         * При этом сам юзер у себя не отображается  */
                        val userUuid = encProfileStorage.getUserUuid()
                        it.members.removeIf { member -> member.userUuid == userUuid || member.role == RoleType.CREATOR }
                        if (encProfileStorage.getRole() != RoleType.CREATOR) {
                            it.members.removeIf { member -> member.role == RoleType.BOSS }
                        }
                        memberRepository.save(it.members)
                        memberRepository.deleteIfNotContains(it.actualIds)
                        viewModel.updateMembers()
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

    enum class InitResult(val failMess: String) {
        SUCCESS(""),                            // Успех
        ENC_PROFILE_NULL(ToastHelper.ERROR_ENC_PROFILE), // Не удалось получить данные профиля для запроса
        FILED(ToastHelper.ERROR_DOWNLOAD_DATA),          // Event не получилось инициализировать, поэтому Fine даже не пытались
        FINES_FILED(ToastHelper.ERROR_DOWNLOAD_FINES)    // Event инициализрованы, Fine не удалось
    }

    enum class UpdateResult {
        SUCCESS, FAIL, NOT_CONSIST_IN_SPACE
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
