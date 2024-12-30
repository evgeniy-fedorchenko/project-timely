package com.efedorchenko.timely.data

import com.efedorchenko.timely.model.MonthUID

interface DataRepository<T> {

    fun save(vararg data: T)

    fun save(data:  T): Long

    // TODO: Проверить, может быть стоит возвращать immutableList
    fun findByMonth(monthUID: MonthUID, withComment: Boolean): MutableList<T>

    fun deleteById(id: Long?): Boolean

}