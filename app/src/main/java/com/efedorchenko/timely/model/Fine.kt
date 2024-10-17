package com.efedorchenko.timely.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.threeten.bp.LocalDate

@Serializable
data class Fine(
    var id: Long?,
    @Contextual val receiptDate: LocalDate,
    val description: String,
    val amount: Int
)
