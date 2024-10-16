package com.efedorchenko.timely.model

import org.threeten.bp.LocalDate

data class Fine(
    var id: Long?,
    val receiptDate: LocalDate,
    val description: String,
    val amount: Int
)
