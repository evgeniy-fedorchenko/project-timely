package com.efedorchenko.timely.model

import com.efedorchenko.timely.model.serializer.YearMonthSerializer
import kotlinx.serialization.Serializable
import org.threeten.bp.YearMonth

@Serializable
data class DataRangeRequest(

    @Serializable(with = YearMonthSerializer::class)
    val startInclusive: YearMonth,

    @Serializable(with = YearMonthSerializer::class)
    val endInclusive: YearMonth,

    val requestedUserId: String? = null
) {

    companion object {
        fun createFirst(userId: String?): DataRangeRequest {
            val startInclusive = YearMonth.now().minusMonths(10L)
            val endInclusive = YearMonth.now().plusMonths(10L)
            return DataRangeRequest(startInclusive, endInclusive, userId)
        }
    }
}
