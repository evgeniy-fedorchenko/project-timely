package com.efedorchenko.timely.model

import com.efedorchenko.timely.model.serializer.YearMonthSerializer
import kotlinx.serialization.Serializable
import org.threeten.bp.YearMonth

@Serializable
data class DataRangeRequest(

    @Serializable(with = YearMonthSerializer::class)
    private val startInclusive: YearMonth,

    @Serializable(with = YearMonthSerializer::class)
    private val endInclusive: YearMonth,

    private val requestedUserId: String
)
