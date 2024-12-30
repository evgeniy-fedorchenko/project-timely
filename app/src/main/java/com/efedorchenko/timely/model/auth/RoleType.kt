package com.efedorchenko.timely.model.auth

import kotlinx.serialization.Serializable

@Serializable
enum class RoleType(val weight: Int) {

    WORKER(10),
    BOSS(20),
    CREATOR(30);

    fun isPrivileged(): Boolean {
        return this != WORKER
    }
}