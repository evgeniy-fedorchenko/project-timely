package com.efedorchenko.timely.model.auth

import kotlinx.serialization.Serializable

@Serializable
enum class RoleType(private val weight: Int) {

    WORKER(10),
    BOSS(20),
    CREATOR(30);

    fun isPrivileged() = this != WORKER
    fun isHigherThan(other: RoleType) = this.weight > other.weight

}