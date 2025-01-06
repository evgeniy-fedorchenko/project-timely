package com.efedorchenko.timely.data

import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.MonthUID

interface DataRepository<T : AbstractData> {

    /**
     * Сохранить новое событие через `insert`, (без `backend_id`).
     * Сгенерировать новый `id (pk)`
     */
    fun save(data: T): Long

    /**
     * Сохранить пачку новых событий через `insert` в одной транзакции (с `backend_id`).
     * Сгенерировать новые `id (pk)`
     */
    fun saveBatch(dataBatch: List<T>)

    /**
     * Сохранить новое событие через `insertWithOnConflict (CONFLICT_REPLACE)` (защита от перезаписи).
     * Должен выполняться после синхронизации с сервером
     */
    fun upsert(data: T): Long

    fun findByMonth(monthUID: MonthUID, withComment: Boolean): List<T>

    fun deleteById(id: Long?): Boolean

    fun setBackendId(data: T)

    fun findNullableBackendId(): List<T>

    fun clean()
}
