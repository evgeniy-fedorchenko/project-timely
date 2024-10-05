package com.efedorchenko.timely.data

import org.threeten.bp.LocalDate

class MonthUID private constructor(private val value: Int) {

    companion object {
        fun create(date: LocalDate): MonthUID {
            val uid = date.year * 100 + date.monthValue
            return MonthUID(uid)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as MonthUID
        return value == other.value
    }

    override fun hashCode(): Int {
        return value
    }

}
