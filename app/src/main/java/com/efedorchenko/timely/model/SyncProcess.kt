package com.efedorchenko.timely.model

data class SyncProcess(
    val eventsOutOfSync: List<AbstractData>?,
    val finesOutOfSync: List<AbstractData>?
) {

    private var _eventsNotSyncCount: Int = eventsOutOfSync?.size ?: 0
    private var _finesNotSyncCount: Int = finesOutOfSync?.size ?: 0

    var isRemoteDataAccepted = true
    var isRemoteMembersAccepted: UpdateResult = UpdateResult.SUCCESS

    fun eventsDec() = --_eventsNotSyncCount
    fun finesDec() = --_finesNotSyncCount

    fun isSuccess(): Boolean {
        return _eventsNotSyncCount == 0
                && _finesNotSyncCount == 0
                && isRemoteDataAccepted
                && isRemoteMembersAccepted == UpdateResult.SUCCESS
    }

    fun getResult(): Result {
        return Result(
            isRemoteDataAccepted = isRemoteDataAccepted,
            isRemoteMembersAccepted = isRemoteMembersAccepted,
            eventsNotSyncSize = _eventsNotSyncCount,
            finesNotSyncSize = _finesNotSyncCount
        )
    }

    class Result private constructor(
        val isRemoteDataAccepted: Boolean,
        val isRemoteMembersAccepted: UpdateResult,
        val eventsNotSyncCount: Int,
        val finesNotSyncCount: Int
    ) {
        companion object {
            operator fun invoke(
                isRemoteDataAccepted: Boolean,
                isRemoteMembersAccepted: UpdateResult,
                eventsNotSyncSize: Int,
                finesNotSyncSize: Int
            ): Result = Result(
                isRemoteDataAccepted,
                isRemoteMembersAccepted,
                eventsNotSyncSize,
                finesNotSyncSize
            )
        }
    }

    enum class UpdateResult {
        SUCCESS, FAIL, NOT_CONSIST_IN_SPACE
    }
}
