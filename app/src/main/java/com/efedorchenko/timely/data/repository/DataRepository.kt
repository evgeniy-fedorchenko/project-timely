package com.efedorchenko.timely.data.repository

import android.app.Application
import android.content.ContentValues
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.BACKEND_ID_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.CHANGED_AT_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.ID_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.TAG
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.USER_UUID_COLUMN_NAME
import com.efedorchenko.timely.model.AbstractData
import com.efedorchenko.timely.model.MonthUID
import org.threeten.bp.Instant

abstract class DataRepository<T : AbstractData>(application: Application) {

    private val dbHelper = DatabaseConfigurer.getInstance(application)

    abstract fun findByMonth(monthUID: MonthUID, withComment: Boolean, userUuid: String? = null): List<T>

    abstract fun findNullableBackendId(): List<T>

    protected abstract fun getTableName(forMembersData: Boolean = false): String

    protected abstract fun extractContentValues(data: T, userUuid: String? = null): ContentValues

    /**
     * Сохранить новое событие через `insert`, (без `changed_at`).
     * Сгенерировать новый `id (pk)`
     */
    fun save(data: T, userUuid: String? = null): T? = saveOne(
        writableDb = dbHelper.writableDatabase,
        tableName = getTableName(userUuid != null),
        data = data,
        userUuid = userUuid,
        conflictAlgorithm = SQLiteDatabase.CONFLICT_NONE
    )

    /**
     * Сохранить новое событие через `insertWithOnConflict (CONFLICT_REPLACE)` (защита от перезаписи).
     * Должен выполняться после синхронизации с сервером
     * Если заполнено поле `deletedAt`, то объект будет удален
     */
    fun upsert(data: T, userUuid: String? = null): T? {
        return if (data.deletedAt != null) {
            delete(data)
            return data
        } else {
            saveOne(
                writableDb = dbHelper.writableDatabase,
                tableName = getTableName(userUuid != null),
                data = data,
                userUuid = userUuid,
                conflictAlgorithm = SQLiteDatabase.CONFLICT_REPLACE
            )
        }
    }

    /**
     * Сохранить пачку новых событий через `insert` в одной транзакции.
     * Сгенерировать новые `id (pk)`
     */
    fun saveBatch(dataBatch: List<T>, userUuid: String? = null) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val tableName = getTableName(userUuid != null)
            dataBatch.forEach {
                saveOne(
                    writableDb = dbHelper.writableDatabase,
                    tableName = tableName,
                    data = it,
                    userUuid = userUuid,
                    conflictAlgorithm = SQLiteDatabase.CONFLICT_NONE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Сохранить пачку новых событий через `insertWithOnConflict(each, CONFLICT_REPLACE)` в одной транзакции.
     * Сгенерировать новые `id (pk)`
     */
    fun upsertBatch(dataBatch: List<T>, userUuid: String? = null) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val tableName = getTableName(userUuid != null)
            dataBatch.forEach {
                if (it.deletedAt != null) {
                        delete(it)
                } else {
                    saveOne(db, tableName, it, userUuid, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun exist(userUuid: String): Boolean {
        val sql = "SELECT 1 FROM ${getTableName(true)} WHERE $USER_UUID_COLUMN_NAME = ? LIMIT 1"
        return dbHelper.readableDatabase.rawQuery(sql, arrayOf(userUuid))
            .use { cursor -> cursor.moveToFirst() }
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

    fun delete(data: T): Boolean {
        val db = dbHelper.writableDatabase
        val deletedRows: Int

        if (data.appId != null) {
            deletedRows = db.delete(getTableName(), "$ID_COLUMN_NAME = ?", arrayOf(data.appId.toString()))
        } else if (data.backendId != null) {
            val whereArgs = arrayOf(data.backendId.toString())
            deletedRows = db.delete(getTableName(), "$BACKEND_ID_COLUMN_NAME = ?", whereArgs)
        } else {
            Log.e(TAG, "No data was deleted, appId and backendId are null. Data: $data")
            return false
        }

        if (deletedRows > 0) {
            return true
        } else {
            Log.e(TAG, "No data was deleted. Data: $data")
            return false
        }
    }

    fun clean() {
        dbHelper.writableDatabase.delete(getTableName(true), null, null)
        dbHelper.writableDatabase.delete(getTableName(false), null, null)
    }

    /**
     * Сохранить единичный объект в переданную БД и вернуть этот же объект.
     * Если в объекте `data` отсутствует `appId`, объект будет сохранен, как новый и возвращен с заполненным `appId`,
     * иначе запись под указанным `id` будет обновлена с указанным `conflictAlgorithm`
     */
    private fun saveOne(
        writableDb: SQLiteDatabase, tableName: String, data: T, userUuid: String? = null, conflictAlgorithm: Int
    ): T? {

        try {
            val values = extractContentValues(data, userUuid)
            val id = writableDb.insertWithOnConflict(tableName, null, values, conflictAlgorithm)
            data.appId = id
            return data
        } catch (ex: SQLException) {
            Log.e(TAG, "Error when insert data with conflict algorithm [$conflictAlgorithm] data: $data. Ex: $ex")
            return null
        }
    }

    //    Для отладки
    fun getAll(tableName: String): List<Map<String, String>> {
        val resultList = mutableListOf<Map<String, String>>()
        val cursor = dbHelper.readableDatabase.rawQuery("SELECT * FROM $tableName", null)
        val columnNames = cursor.columnNames

        while (cursor.moveToNext()) {
            val rowMap = columnNames.mapIndexed { index, columnName ->
                columnName to (cursor.getString(index) ?: "null")
            }.toMap()
            resultList.add(rowMap)
        }

        cursor.close()
        return resultList
    }
}
