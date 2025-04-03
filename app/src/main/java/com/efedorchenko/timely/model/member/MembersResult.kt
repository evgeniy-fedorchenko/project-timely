package com.efedorchenko.timely.model.member

import com.efedorchenko.timely.model.auth.SpaceDto
import kotlinx.serialization.Serializable

@Serializable
data class MembersResult(

    val spaceStatus: SpaceStatus = SpaceStatus.NONE,
    val space: SpaceDto? = null,
    val members: MutableList<SpaceMember>,

    /**
     * Список `id` всех участников данного пространства.
     *
     * Присылается для понимания, какие участники были удалены из пространства. В то время как [members] содержит
     * только новых (или обновленных участников) и не содержит информации об участниках, которые были удалены.
     * При получении этого списка в локальном хранилище должны быть найдены все юзеры, `id` отсутствуют в данном
     * списке - они должны быть удалены, так как были удалены из целевого пространства
     */
    val actualIds: List<String>
)
