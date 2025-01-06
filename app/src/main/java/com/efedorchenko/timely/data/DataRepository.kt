package com.efedorchenko.timely.data

import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.MonthUID

interface DataRepository<T : AbstractData> {

    /**
     * Сохранить новое событие через `insert`, (без `backend_id`).
     * Сгенерировать новый `id (pk)`
     */
    fun save(data:  T): Long

    /**
     * Сохранить пачку новых событий через `insert` в одной транзакции (с `backend_id`).
     * Сгенерировать новые `id (pk)`
     */
    fun saveBatch(dataBatch: List<T>)

    fun findByMonth(monthUID: MonthUID, withComment: Boolean): List<T>

    fun deleteById(id: Long?): Boolean

    fun setBackendId(data: T)

    fun findNullableBackendId(): List<T>
}