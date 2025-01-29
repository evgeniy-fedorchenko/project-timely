package com.efedorchenko.timely.model

data class UserDataModifyDto(

    val modifyingUserId: String,
    val newData: AbstractData
)
