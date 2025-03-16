package com.efedorchenko.timely.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive

class SyncOperator(
    val eventsOutOfSync: List<AbstractData>?,
    val finesOutOfSync: List<AbstractData>?,
    private val executor: suspend (SyncOperator, CoroutineScope) -> Unit,
    private val failureHandler: (Result) -> Unit,
    private val successHandler: () -> Unit
) {

    private var _eventsNotSyncCount: Int = eventsOutOfSync?.size ?: 0
    private var _finesNotSyncCount: Int = finesOutOfSync?.size ?: 0

    var isRemoteDataAccepted = true
    var getRemoteMembersResult: UpdateResult = UpdateResult.SUCCESS

    fun eventsDec() = --_eventsNotSyncCount
    fun finesDec() = --_finesNotSyncCount

    suspend fun start(scope: CoroutineScope) {
        if (scope.isActive) executor.invoke(this, scope)
    }

    fun handleResult(isActive: Boolean) {
        val isSuccess = (_eventsNotSyncCount == 0
                && _finesNotSyncCount == 0
                && isRemoteDataAccepted
                && getRemoteMembersResult == UpdateResult.SUCCESS)

        if (isSuccess) {
            successHandler.invoke()
            return
        }
        if (isActive) {
            val result = Result(
                isRemoteDataAccepted = isRemoteDataAccepted,
                getRemoteMembersResult = getRemoteMembersResult,
                eventsNotSyncSize = _eventsNotSyncCount,
                finesNotSyncSize = _finesNotSyncCount
            )
            failureHandler.invoke(result)
        }
    }

    class Result private constructor(
        val isRemoteDataAccepted: Boolean,
        val getRemoteMembersResult: UpdateResult,
        val eventsNotSyncCount: Int,
        val finesNotSyncCount: Int
    ) {
        companion object {
            operator fun invoke(
                isRemoteDataAccepted: Boolean,
                getRemoteMembersResult: UpdateResult,
                eventsNotSyncSize: Int,
                finesNotSyncSize: Int
            ): Result = Result(
                isRemoteDataAccepted,
                getRemoteMembersResult,
                eventsNotSyncSize,
                finesNotSyncSize
            )
        }
    }

    enum class UpdateResult {
        SUCCESS, FAIL, NOT_CONSIST_IN_SPACE
    }
}
