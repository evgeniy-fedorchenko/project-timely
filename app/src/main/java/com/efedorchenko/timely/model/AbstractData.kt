package com.efedorchenko.timely.model

import com.efedorchenko.timely.model.serializer.LocalDateSerializer
import kotlinx.serialization.Serializable
import org.threeten.bp.LocalDate

@Serializable
sealed class AbstractData {

    abstract var appId: Long?

    abstract var backendId: Long?

    @Serializable(with = LocalDateSerializer::class)
    abstract val date: LocalDate

    abstract fun getType(): DataType

    @Suppress("unchecked_cast")
    fun <T : AbstractData> toInheritor(): T = this as T
}
