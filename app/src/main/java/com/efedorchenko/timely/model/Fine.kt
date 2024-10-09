package com.efedorchenko.timely.model

import org.threeten.bp.LocalDate

data class Fine(
    val receiptDate: LocalDate,
    val description: String,
    val amount: Int
)
