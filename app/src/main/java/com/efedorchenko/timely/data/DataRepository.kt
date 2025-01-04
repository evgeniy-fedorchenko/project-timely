package com.efedorchenko.timely.data

import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.MonthUID

interface DataRepository<T : AbstractData> {

    fun save(data:  T): Long

    fun findByMonth(monthUID: MonthUID, withComment: Boolean): List<T>

    fun deleteById(id: Long?): Boolean

    fun setBackendId(data: T)

    fun findNullableBackendId(): List<T>
}