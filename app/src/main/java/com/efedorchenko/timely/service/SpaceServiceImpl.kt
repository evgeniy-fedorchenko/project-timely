package com.efedorchenko.timely.service

import android.util.Log
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.MemberRepository
import com.efedorchenko.timely.data.RepositoryFactory
import com.efedorchenko.timely.model.DataRangeRequest
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.model.SpaceMember
import com.efedorchenko.timely.model.api.ApiResponse
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
        when (val response = apiService.getMembers()) {
            is ApiResponse.Success -> {
                response.data?.let {
                    memberRepository.save(it)
                    viewModel.updateMembers()
                    return true
                }
                return false
            }
            is ApiResponse.Error -> {
                Log.e("Network error", "Cannot get members from server." +
                        "ApiErrorCode: ${response.apiErrorCode}, " +
                        "error message: ${response.errorMessage}, " +
                        "error data: ${response.errorData}"
                )
                return false
            }
        }
    }

    override fun downloadMember(member: SpaceMember) {
        TODO("Not yet implemented")
    }

    override suspend fun initData(): InitResult {
        val userUuid = encProfileStorage.getUserUuid() ?: return InitResult.ENC_PROFILE_NULL

        val startInclusive = YearMonth.now().minusMonths(2L)
        val endInclusive = YearMonth.now().plusMonths(1L)

        val dataRangeRequest = DataRangeRequest(startInclusive, endInclusive, userUuid)
        if (!doInit(dataRangeRequest, DataType.EVENT)) {
            return InitResult.FILED
        }
        if (!doInit(dataRangeRequest, DataType.FINE)) {
            return InitResult.FINES_FILED
        }
        return InitResult.SUCCESS
    }

    private suspend fun doInit(dataRequest: DataRangeRequest, dataType: DataType): Boolean {
        when (val response = apiService.getDataRange(dataRequest, dataType)) {
            is ApiResponse.Success -> {
                response.data?.let {
                    if (it.isNotEmpty()) {
                        val repository = repositoryFactory.getRepository(it[0])
                        repository.saveBatch(it)
                        viewModel.updateLiveData(dataType, LocalDate.now())
                        viewModel.emitNeedUpdateData()
                    }
                    return true
                }
            }
            is ApiResponse.Error -> {
                Log.e("Network error", "Cannot init $dataType." +
                        "ApiErrorCode: ${response.apiErrorCode}, " +
                        "error message: ${response.errorMessage}, " +
                        "error data: ${response.errorData}"
                )
                return false
            }
        }
        return false
    }

    enum class InitResult(val failMess: String) {

        SUCCESS(""),                            // Успех
        ENC_PROFILE_NULL(ToastHelper.ERROR_ENC_PROFILE), // Не удалось получить данные профиля для запроса
        FILED(ToastHelper.ERROR_DOWNLOAD_DATA),          // Event не получилось инициализировать, поэтому Fine даже не пытались
        FINES_FILED(ToastHelper.ERROR_DOWNLOAD_FINES)    // Event инициализрованы, Fine не удалось
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
