package com.efedorchenko.timely.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {

    WORKER, BOSS, CREATOR;

    fun isPrivileged(): Boolean {
        return this != WORKER
    }
}