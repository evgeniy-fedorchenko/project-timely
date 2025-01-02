package com.efedorchenko.timely.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val role: RoleType,
    val name: String,
    val position: String,
    val rate: Int?,
    val creatingSpace: SpaceCreateDto?,
    val spaceKey: String?,
) {

    companion object {
        fun build(block: Builder.() -> Unit): RegisterRequest {
            return Builder().apply(block).build()
        }
    }

    @DslMarker
    annotation class BuilderDsl

    @BuilderDsl
    class Builder {
        var username: String = ""
        var password: String = ""
        var role: RoleType = RoleType.WORKER
        var name: String = ""
        var position: String = ""
        var rate: Int? = null
        var creatingSpace: SpaceCreateDto? = null
        var spaceKey: String? = null

        fun build() = RegisterRequest(
            username = username,
            password = password,
            role = role,
            name = name,
            position = position,
            rate = rate,
            creatingSpace = creatingSpace,
            spaceKey = spaceKey
        )
    }
}