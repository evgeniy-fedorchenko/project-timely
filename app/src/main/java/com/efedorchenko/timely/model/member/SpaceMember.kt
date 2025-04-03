package com.efedorchenko.timely.model.member

import com.efedorchenko.timely.model.auth.RoleType
import com.efedorchenko.timely.model.serializer.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.threeten.bp.Instant

@Serializable
data class SpaceMember(

    @SerialName("userId")
    val userUuid: String,

    val name: String,

    val position: String,

    val role: RoleType? = null, // TODO: Проверить, может стоит заполнять эти поля при взятии юзера из таблиы members

    var rate: Int? = null,

    var spaceStatus: SpaceStatus,

//    store as epoch milli
    @Serializable(InstantSerializer::class)
    val changedAt: Instant? = null
) {

    fun isPrivileged() = this.role?.isPrivileged() == true
    fun isCreator()  = this.role == RoleType.CREATOR
    fun isPendingMember() = this.spaceStatus.isPending()
    fun isPendingBoss() = this.spaceStatus == SpaceStatus.PENDING_BOSS
}
