package com.efedorchenko.timely.model.auth

import kotlinx.serialization.Serializable

@Serializable
enum class AuthErrorCode(val description: String) {

    /* 200 */ OK("ОК"),
    /* 202 */ ACCEPTED("Ваша заявка на участие в пространстве принята"),   // Default
    /* 409 */ ALREADY_REGISTERED("Пользователь с таким email уже существует"),
    /* 401 */ UNREGISTERED("Такого пользователя не сущетсвует. Проверьте корректность введенных данных"),
    /* 404 */ SPACE_NOT_FOUND("Указанное пространство не найдено. Проверьте корректность введенных данных"),
    /* 403 */ SPACE_CREATION_PROHIBITED("Вам не разрешено создавать новые пространства");
}