package com.efedorchenko.timely.model.auth

import kotlinx.serialization.Serializable

@Serializable
enum class AuthErrorCode(val description: String) {

    /* 409 */ ALREADY_REGISTERED("Это имя пользователя уже занято"),
    /* 401 */ UNREGISTERED("Такого пользователя не сущетсвует. Проверьте корректность введенных данных"),
    /* 404 */ SPACE_NOT_FOUND("Указанное пространство не найдено. Проверьте корректность введенных данных"),
    /* 403 */ SPACE_CREATION_PROHIBITED("Вам не разрешено создавать новые пространства");

}