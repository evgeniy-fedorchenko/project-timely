package com.efedorchenko.timely.data

import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.MonthUID
import org.threeten.bp.Instant

interface DataRepository<T : AbstractData> {

    /**
     * Сохранить новое событие через `insert`, (без `changed_at`).
     * Сгенерировать новый `id (pk)`
     */
    fun save(data: T): Long

    /**
     * Сохранить новое событие через `insertWithOnConflict (CONFLICT_REPLACE)` (защита от перезаписи).
     * Должен выполняться после синхронизации с сервером
     */
    fun upsert(data: T): Long

    /**
     * Сохранить пачку новых событий через `insert` в одной транзакции (с `changed_at`).
     * Сгенерировать новые `id (pk)`
     */
    fun saveBatch(dataBatch: List<T>)

    /**
     * Сохранить пачку новых событий через `insertWithOnConflict (CONFLICT_REPLACE)`
     * в одной транзакции (с `changed_at`). Сгенерировать новые `id (pk)`
     */
    fun upsertBatch(dataBatch: List<T>)

    fun findByMonth(monthUID: MonthUID, withComment: Boolean): List<T>

    fun setBackendProperties(data: T)

    fun findNullableBackendId(): List<T>

    fun getMaxChangedAt(): Instant?

    fun deleteById(id: Long): Boolean

    fun clean()
}
