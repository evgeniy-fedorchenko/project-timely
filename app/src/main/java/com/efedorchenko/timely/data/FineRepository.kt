package com.efedorchenko.timely.data

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.core.content.contentValuesOf
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.AMOUNT_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.BACKEND_ID_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.CHANGED_AT_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.COMMENT_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.DATE_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.DESCRIPTION_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.FINES_TABLE_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.ID_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.MONTH_UID_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.TAG
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.model.MonthUID
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import javax.inject.Inject

class FineRepository @Inject constructor(application: Application) : DataRepository<Fine> {

    companion object {
        private const val SELECT_FINES_BY_MONTH_UID =         "SELECT * FROM $FINES_TABLE_NAME WHERE $MONTH_UID_COLUMN_NAME = ?"
        private const val SELECT_FINES_WITH_NULL_BACKEND_ID = "SELECT * FROM $FINES_TABLE_NAME WHERE $BACKEND_ID_COLUMN_NAME IS NULL"
        private const val SELECT_MAX_CHANGED_AT =             "SELECT MAX($CHANGED_AT_COLUMN_NAME) FROM $FINES_TABLE_NAME"
    }

    private val dbHelper = DatabaseConfigurer.getInstance(application)

    override fun save(data: Fine): Long = saveOne(dbHelper.writableDatabase, data, SQLiteDatabase.CONFLICT_NONE)

    override fun upsert(data: Fine): Long {
        return if (data.deletedAt != null) {
            if (data.appId?.let { deleteById(it) } == true) {
                data.appId ?: -1
            } else -1
        } else {
            saveOne(dbHelper.writableDatabase, data, SQLiteDatabase.CONFLICT_NONE)
        }
    }

    override fun saveBatch(dataBatch: List<Fine>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            dataBatch.forEach { saveOne(db, it, SQLiteDatabase.CONFLICT_NONE) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun upsertBatch(dataBatch: List<Fine>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            dataBatch.forEach {
                if (it.deletedAt != null) {
                    it.appId?.let { it1 -> deleteById(it1) }
                } else {
                    saveOne(db, it, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Возвращается без `backend_id` и `changed_at`
     * @param withComment - игнорироуется, объекты всегда возвращаются с описанием
     */
    override fun findByMonth(monthUID: MonthUID, withComment: Boolean): List<Fine> {
        val db = dbHelper.readableDatabase
        val fines = mutableListOf<Fine>()
        var cursor: Cursor? = null
        db.beginTransaction()

        try {
            cursor = db.rawQuery(SELECT_FINES_BY_MONTH_UID, arrayOf(monthUID.hashCode().toString()))
                ?.run {
                    while (moveToNext()) {
                        val id = columnAs(ID_COLUMN_NAME) { idx -> getLong(idx) }
                        val date = columnAs(DATE_COLUMN_NAME) { idx -> getString(idx) }
                        val amount = columnAs(AMOUNT_COLUMN_NAME) { idx -> getInt(idx) }
                        val description = columnAs(COMMENT_COLUMN_NAME) { idx -> getString(idx) }

                        if (amount != null && description != null) {
                            val fine = Fine(
                                appId = id,
                                date = LocalDate.parse(date),
                                amount = amount,
                                description = description
                            )
                            fines.add(fine)
                        }
                    }
                    this
                }
            db.setTransactionSuccessful()
        } catch (ex: Exception) {
            Log.e(TAG, "Error when finding fines by month uid. Ex: :$ex")
        } finally {
            cursor?.close()
            db.endTransaction()
        }
        return fines
    }

    override fun setBackendProperties(data: Fine) {
        dbHelper.writableDatabase.update(
            FINES_TABLE_NAME,
            contentValuesOf(Pair(CHANGED_AT_COLUMN_NAME, data.changedAt), Pair(BACKEND_ID_COLUMN_NAME, data.backendId)),
            "$ID_COLUMN_NAME = ?",
            arrayOf(data.appId.toString())
        )
    }

    override fun findNullableBackendId(): List<Fine> {
        val db = dbHelper.readableDatabase
        val fines = mutableListOf<Fine>()
        var cursor: Cursor? = null
        db.beginTransaction()

        try {
            cursor = db.rawQuery(SELECT_FINES_WITH_NULL_BACKEND_ID, null)
                ?.run {
                    while (moveToNext()) {
                        val id = columnAs(ID_COLUMN_NAME) { idx -> getLong(idx) }
                        val date = columnAs(DATE_COLUMN_NAME) { idx -> getString(idx) }
                        val amount = columnAs(AMOUNT_COLUMN_NAME) { idx -> getInt(idx) }
                        val description = columnAs(COMMENT_COLUMN_NAME) { idx -> getString(idx) }

                        if (amount != null && description != null) {
                            val fine = Fine(
                                appId = id,
                                date = LocalDate.parse(date),
                                amount = amount,
                                description = description
                            )
                            fines.add(fine)
                        }
                    }
                    this
                }
            db.setTransactionSuccessful()
        } catch (ex: Exception) {
            Log.e(TAG, "Error when extracting fines with nullable backendId. Ex :$ex")
        } finally {
            cursor?.close()
            db.endTransaction()
        }
        return fines
    }

    override fun getMaxChangedAt(): Instant? {
        return dbHelper.readableDatabase.rawQuery(SELECT_MAX_CHANGED_AT, null)
            .use { cursor ->
                if (cursor.moveToFirst()) {
                    val maxTime = cursor.getLong(0)
                    if (maxTime > 0) {
                        Instant.ofEpochMilli(maxTime)
                    } else null
                } else null
            }
    }

    override fun deleteById(id: Long): Boolean {
        val db = dbHelper.writableDatabase
        val deletedRows = db.delete(FINES_TABLE_NAME, "$ID_COLUMN_NAME = ?", arrayOf(id.toString()))

        if (deletedRows == 0) {
            return true
        } else {
            Log.e(TAG, "No event was deleted with id: $id. Ex: ")
            return false
        }
    }

    override fun clean() {
        dbHelper.writableDatabase.delete(FINES_TABLE_NAME, null, null)
    }

    private fun saveOne(writableDb: SQLiteDatabase, fine: Fine, conflictAlgorithm: Int): Long {
        try {
            val values = extractContentValues(fine)
            return writableDb.insertWithOnConflict(FINES_TABLE_NAME, null, values, conflictAlgorithm)
        } catch (ex: SQLException) {
            Log.e(TAG, "Error when insert fine with conflict algorithm [$conflictAlgorithm] fine: $fine. Ex: $ex")
            return -1
        }
    }

    private fun extractContentValues(fine: Fine): ContentValues {
        return ContentValues().apply {
            fine.appId?.let { put(ID_COLUMN_NAME, fine.appId) }
            fine.backendId?.let { put(BACKEND_ID_COLUMN_NAME, it) }
            put(DATE_COLUMN_NAME, fine.date.toString())
            put(MONTH_UID_COLUMN_NAME, MonthUID.create(fine.date).value)
            fine.changedAt?.let { put(CHANGED_AT_COLUMN_NAME, it.toEpochMilli()) }

            put(DESCRIPTION_COLUMN_NAME, fine.description)
            put(AMOUNT_COLUMN_NAME, fine.amount)
        }
    }
}
