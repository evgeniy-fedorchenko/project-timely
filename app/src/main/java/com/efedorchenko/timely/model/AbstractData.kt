package com.efedorchenko.timely.model

import com.efedorchenko.timely.model.serializer.InstantSerializer
import com.efedorchenko.timely.model.serializer.LocalDateSerializer
import kotlinx.serialization.Serializable
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate

@Serializable
sealed class AbstractData {

    abstract var appId: Long?

    abstract var backendId: Long?

    @Serializable(with = LocalDateSerializer::class)
    abstract val date: LocalDate

    @Serializable(with = InstantSerializer::class)
    abstract val deletedAt: Instant?

    @Serializable(with = InstantSerializer::class)
    abstract val changedAt: Instant?

    abstract fun getType(): DataType

    var toUserId: String? = null

    abstract fun logicEquals(other: Any): Boolean
}
