package com.efedorchenko.timely.model.auth

import kotlinx.serialization.Serializable

@Serializable
enum class RoleType(val weight: Int, val title: String) {

    WORKER(10, "Работник"),
    BOSS(20, "Руководитель"),
    CREATOR(30, "Создатель пространства");

    fun isPrivileged(): Boolean {
        return this != WORKER
    }

    fun isHigherThan(other: RoleType): Boolean {
        return this.weight > other.weight
    }
}