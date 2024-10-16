package com.efedorchenko.timely.repository

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.model.MonthUID
import com.efedorchenko.timely.repository.DatabaseConfigurer.Companion.AMOUNT_COLUMN_NAME
import com.efedorchenko.timely.repository.DatabaseConfigurer.Companion.DESCRIPTION_COLUMN_NAME
import com.efedorchenko.timely.repository.DatabaseConfigurer.Companion.FINES_TABLE_NAME
import com.efedorchenko.timely.repository.DatabaseConfigurer.Companion.ID_COLUMN_NAME
import com.efedorchenko.timely.repository.DatabaseConfigurer.Companion.MONTH_UID_COLUMN_NAME
import com.efedorchenko.timely.repository.DatabaseConfigurer.Companion.RECEIPT_DATE_COLUMN_NAME
import org.threeten.bp.LocalDate

class FineRepository(private val application: Application) {

    private val dbHelper = DatabaseConfigurer.getInstance(application)

    fun save(vararg fines: Fine) {
        fines.forEach { save(it) }
    }

    fun save(fine: Fine): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(MONTH_UID_COLUMN_NAME, MonthUID.create(fine.receiptDate).hashCode())
            put(RECEIPT_DATE_COLUMN_NAME, fine.receiptDate.toString())
            put(DESCRIPTION_COLUMN_NAME, fine.description)
            put(AMOUNT_COLUMN_NAME, fine.amount)
        }

        val id = db.insert(FINES_TABLE_NAME, null, values)
        if (id == -1L) {
            Log.e("InsertError", "Error when insert fine ${fine}")
        }
        return id
    }

    fun findByMonth(monthUID: MonthUID): List<Fine> {
        val fines = mutableListOf<Fine>()
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null

        db.beginTransaction()
        try {
            cursor = db.query(
                FINES_TABLE_NAME,
                null,
                "$MONTH_UID_COLUMN_NAME = ?",
                arrayOf(monthUID.hashCode().toString()),
                null,
                null,
                null
            )

            cursor?.let {
                while (cursor.moveToNext()) {

                    val idIdx = cursor.getColumnIndex(ID_COLUMN_NAME)
                    val receiptDateIdx = cursor.getColumnIndex(RECEIPT_DATE_COLUMN_NAME)
                    val descriptionIdx = cursor.getColumnIndex(DESCRIPTION_COLUMN_NAME)
                    val amountIdx = cursor.getColumnIndex(AMOUNT_COLUMN_NAME)

                    val id = cursor.getLong(idIdx)
                    val receiptDate = cursor.getString(receiptDateIdx)
                    val description = cursor.getString(descriptionIdx)
                    val amount = cursor.getInt(amountIdx)

                    val fine = Fine(id, LocalDate.parse(receiptDate), description, amount)
                    fines.add(fine)
                }
            }
            db.setTransactionSuccessful()
        } catch (ex: Exception) {
            Log.e("DatabaseError", "Error when extracting events. Cause: :${ex.message}")
        } finally {
            cursor?.close()
            db.endTransaction()
        }

        return fines
    }

    fun getAllFines(): List<Fine> {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            FINES_TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            null
        )

        val fines = mutableListOf<Fine>()
        with(cursor) {
            while (cursor.moveToNext()) {
                val idIdx = cursor.getColumnIndex(ID_COLUMN_NAME)
                val receiptDateIdx = cursor.getColumnIndex(RECEIPT_DATE_COLUMN_NAME)
                val descriptionIdx = cursor.getColumnIndex(DESCRIPTION_COLUMN_NAME)
                val amountIdx = cursor.getColumnIndex(AMOUNT_COLUMN_NAME)

                val id = cursor.getLong(idIdx)
                val receiptDate = cursor.getString(receiptDateIdx)
                val description = cursor.getString(descriptionIdx)
                val amount = cursor.getInt(amountIdx)

                val fine = Fine(id, LocalDate.parse(receiptDate), description, amount)
                fines.add(fine)
            }
        }
        cursor.close()
        return fines
    }

    fun deleteById(id: Long?): Boolean {
        val db = dbHelper.writableDatabase

        val deletedRows = db.delete(
            FINES_TABLE_NAME,
            "$ID_COLUMN_NAME = ?",
            arrayOf(id.toString())
        )

        if (deletedRows > 0) {
            return true
        } else {
            Log.e("DeleteError", "No fine was deleted with id: $id")
            return false
        }
    }
}