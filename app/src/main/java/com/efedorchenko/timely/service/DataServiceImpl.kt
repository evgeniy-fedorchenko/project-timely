package com.efedorchenko.timely.service

import android.util.Log
import com.efedorchenko.timely.data.DataViewModel
import com.efedorchenko.timely.data.EncProfileStorage
import com.efedorchenko.timely.data.repository.RepositoryFactory
import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.DataRangeRequest
import com.efedorchenko.timely.model.DataType
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.model.api.ApiResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.YearMonth
import javax.inject.Inject

class DataServiceImpl @Inject constructor(
    private val apiService: ApiService,
    private val repositoryFactory: RepositoryFactory,
    private val viewModel: DataViewModel,
    private val encProfileStorage: EncProfileStorage,
) : DataService {

    companion object {
        private const val NETWORK_ERROR_TAG = "Network error"
    }

    /**
     * Первоначальная загрузка данных юзера
     *
     * В ситуации, когда запрашиваются данные участника пространства (а не авторизованного юзера) и локально
     * по этому юзеру не имеется никаких данных - запрос и сохранение полученных данных выполняется синхронно
     * и возвращается результат загрузки и сохранения - `true`/`false`.
     * Иначе найденные данные загружаются во `viewModel` и возвращается `true`, далее происходит асинхронный
     * запрос новых данных. В случае ошибки вызывается [DataViewModel.emitNotSynced]
     *
     * @param userUuid идентификатор юзера, данные которого нужно загрузить. Если передан `null` - загружается
     * авторизованный пользователь
     */
    override suspend fun loadData(userUuid: String?): Boolean {
        if (userUuid != null && !someDataExist(userUuid)) {
            return getAndSaveData(userUuid)
        }
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.updateLiveData(userUuid = userUuid)
            if (!updateData(userUuid)) {
                viewModel.emitNotSynced.invoke()
            }
        }
        return true
    }

    /**
     * Обновить данные переданного юзера (или авторизованного, если `userUuid == null`)
     *
     * Происходит поиск события с наивысшим временем обновления, далее запрос по http с этим временем - ответ
     * со всеми данными после этого события сохраняется и отображается на UI.
     * Последовательно: сначала для [Event], затем для [Fine]. Штрафы обновляются для авторизованного юзера или если
     * их обновляет администратор. (в противном случае это не имеет смысла, так как юзеру запрещено просматривать
     * чужие штрафы)
     * */
    // TODO: Объединить loadData и updateData - пусть всегда запрашивать since, если его нет - делать обычный getRange
    override suspend fun updateData(userUuid: String?): Boolean {
        val eventsSince = repositoryFactory.get(DataType.EVENT).getMaxChangedAt()
        if (!downloadData(userUuid) { apiService.getUpdates(userUuid, DataType.EVENT, eventsSince) }) {
            return false
        }
        if (!encProfileStorage.isPrivileged() && userUuid != null) {
            return true
        }
        val finesSince = repositoryFactory.get(DataType.FINE).getMaxChangedAt()
        return downloadData(userUuid) { apiService.getUpdates(userUuid, DataType.FINE, finesSince) }
    }

    private suspend fun getAndSaveData(userUuid: String?): Boolean {
        val startInclusive = YearMonth.now().minusMonths(10L)
        val endInclusive = YearMonth.now().plusMonths(10L)
        val requestBody = DataRangeRequest(startInclusive, endInclusive, userUuid)

        if (!downloadData(userUuid) { apiService.getRange(requestBody, DataType.EVENT) }) {
            return false
        }
        if (!encProfileStorage.isPrivileged() && userUuid != null) {
            return true
        }
        return downloadData(userUuid) { apiService.getRange(requestBody, DataType.FINE) }
    }

    private suspend fun downloadData(
        userUuid: String?, requestFunc: suspend () -> ApiResponse<List<AbstractData>>
    ): Boolean {

        when (val response = requestFunc.invoke()) {
            is ApiResponse.Success -> {
                response.data?.let {
                    if (it.isNotEmpty()) {
                        repositoryFactory.getRepository(it[0]).upsertBatch(it, userUuid)
                        viewModel.updateLiveData(it[0].getType(), userUuid)
                    }
                    return true
                }
                return false
            }

            is ApiResponse.Error -> {
                logNetworkError("Cannot request data of member: [$userUuid]", response)
                return false
            }
        }
    }

    private fun someDataExist(userUuid: String): Boolean {
        DataType.entries.forEach {
            if (repositoryFactory.getRepository<AbstractData>(it).exist(userUuid)) {
                return@someDataExist true
            }
        }
        return false
    }

    private fun <T> logNetworkError(uniquePrefix: String, response: ApiResponse.Error<T>) {
        val message = buildString {
            append("$uniquePrefix, ")
            append("requestUid: [${response.rqUid}], ")
            append("apiErrorCode: [${response.apiErrorCode}], ")
            append("errorMessage: [${response.errorMessage}], ")
            append("errorData: [${response.errorData}]")
        }
        Log.e(NETWORK_ERROR_TAG, message)
    }
}