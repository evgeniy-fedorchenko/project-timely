package com.efedorchenko.timely.data

import android.app.Application
import android.content.ContentValues
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.core.content.contentValuesOf
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.BACKEND_ID_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.CHANGED_AT_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.ID_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.TAG
import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.MonthUID
import org.threeten.bp.Instant

abstract class DataRepository<T : AbstractData>(application: Application) {

    private val dbHelper = DatabaseConfigurer.getInstance(application)

    abstract fun findByMonth(monthUID: MonthUID, withComment: Boolean): List<T>

    abstract fun findNullableBackendId(): List<T>

    protected abstract fun getTableName(): String

    protected abstract fun extractContentValues(data: T): ContentValues

    /**
     * Сохранить новое событие через `insert`, (без `changed_at`).
     * Сгенерировать новый `id (pk)`
     */
    fun save(data: T): Long = saveOne(dbHelper.writableDatabase, data, SQLiteDatabase.CONFLICT_NONE)

    /**
     * Сохранить новое событие через `insertWithOnConflict (CONFLICT_REPLACE)` (защита от перезаписи).
     * Должен выполняться после синхронизации с сервером
     */
    fun upsert(data: T): Long {
        return if (data.deletedAt != null) {
            if (data.appId?.let { deleteById(it) } == true) {
                data.appId ?: -1
            } else -1
        } else {
            saveOne(dbHelper.writableDatabase, data, SQLiteDatabase.CONFLICT_NONE)
        }
    }

    /**
     * Сохранить пачку новых событий через `insert` в одной транзакции.
     * Сгенерировать новые `id (pk)`
     */
    fun saveBatch(dataBatch: List<T>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            dataBatch.forEach { saveOne(db, it, SQLiteDatabase.CONFLICT_NONE) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Сохранить пачку новых событий через `insertWithOnConflict(each, CONFLICT_REPLACE)` в одной транзакции.
     * Сгенерировать новые `id (pk)`
     */
    fun upsertBatch(dataBatch: List<T>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            dataBatch.forEach {
                if (it.deletedAt != null) {
                    it.appId?.let { id -> deleteById(id) }
                } else {
                    saveOne(db, it, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun saveOne(writableDb: SQLiteDatabase, data: T, conflictAlgorithm: Int): Long {
        try {
            val values = extractContentValues(data)
            return writableDb.insertWithOnConflict(getTableName(), null, values, conflictAlgorithm)
        } catch (ex: SQLException) {
            Log.e(TAG, "Error when insert data with conflict algorithm [$conflictAlgorithm] data: $data. Ex: $ex")
            return -1
        }
    }

    fun setBackendProperties(data: T) {
        dbHelper.writableDatabase.update(
            getTableName(),
            contentValuesOf(Pair(CHANGED_AT_COLUMN_NAME, data.changedAt), Pair(BACKEND_ID_COLUMN_NAME, data.backendId)),
            "$ID_COLUMN_NAME = ?",
            arrayOf(data.appId.toString())
        )
    }

    fun getMaxChangedAt(): Instant? {
        val sql = "SELECT MAX($CHANGED_AT_COLUMN_NAME) FROM ${getTableName()}"
        return dbHelper.readableDatabase.rawQuery(sql, null)
            .use { cursor ->
                if (cursor.moveToFirst()) {
                    val maxTime = cursor.getLong(0)
                    if (maxTime > 0) Instant.ofEpochMilli(maxTime) else null
                } else null
            }
    }

    fun deleteById(id: Long): Boolean {
            val db = dbHelper.writableDatabase
            val deletedRows = db.delete(getTableName(), "$ID_COLUMN_NAME = ?", arrayOf(id.toString()))

            if (deletedRows == 0) {
                return true
            } else {
                Log.e(TAG, "No data was deleted with id: $id")
                return false
            }
    }

    fun clean() {
        dbHelper.writableDatabase.delete(getTableName(), null, null)
    }

////    Для отладки
//    fun getAllData(): List<Array<String>> {
//        val resultList = mutableListOf<Array<String>>()
//        val db = dbHelper.readableDatabase
//        val cursor = db.rawQuery("SELECT * FROM ${getTableName()}", null)
//
//        while (cursor.moveToNext()) {
//            val row = Array(cursor.columnCount) { index ->
//                cursor.getString(index) ?: "null"
//            }
//            resultList.add(row)
//        }
//
//        cursor.close()
//        db.close()
//
//        return resultList
//    }
}
