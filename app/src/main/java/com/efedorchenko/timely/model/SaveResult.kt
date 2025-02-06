package com.efedorchenko.timely.model

sealed class SaveResult {

    data object Success : SaveResult()
    data object SyncFiled : SaveResult()
    data class ServerChanged(val newData: AbstractData) : SaveResult()
    data object Error : SaveResult()
}