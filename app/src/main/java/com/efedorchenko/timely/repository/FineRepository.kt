package com.efedorchenko.timely.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import com.efedorchenko.timely.data.Fine
import com.efedorchenko.timely.data.MonthUID
import com.efedorchenko.timely.repository.DatabaseConfigurer.Companion.AMOUNT_COLUMN_NAME
import com.efedorchenko.timely.repository.DatabaseConfigurer.Companion.DESCRIPTION_COLUMN_NAME
import com.efedorchenko.timely.repository.DatabaseConfigurer.Companion.FINES_TABLE_NAME
import com.efedorchenko.timely.repository.DatabaseConfigurer.Companion.MONTH_UID_COLUMN_NAME
import com.efedorchenko.timely.repository.DatabaseConfigurer.Companion.RECEIPT_DATE_COLUMN_NAME
import org.threeten.bp.LocalDate

class FineRepository(private val application: Application) {

    private val dbHelper = DatabaseConfigurer.getInstance(application)

    fun save(vararg fines: Fine) {
        fines.forEach { save(it) }
    }

    fun save(fine: Fine) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(MONTH_UID_COLUMN_NAME, MonthUID.create(fine.receiptDate).hashCode())
            put(RECEIPT_DATE_COLUMN_NAME, fine.receiptDate.toString())
            put(DESCRIPTION_COLUMN_NAME, fine.description)
            put(AMOUNT_COLUMN_NAME, fine.amount)

        }
        db.insert(FINES_TABLE_NAME, null, values)
    }

    fun findByMonth(monthUID: MonthUID): MutableList<Fine> {
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
                    val receiptDateIdx = cursor.getColumnIndex(RECEIPT_DATE_COLUMN_NAME)
                    val descriptionIdx = cursor.getColumnIndex(DESCRIPTION_COLUMN_NAME)
                    val amountIdx = cursor.getColumnIndex(AMOUNT_COLUMN_NAME)

                    val receiptDate = cursor.getString(receiptDateIdx)
                    val description = cursor.getString(descriptionIdx)
                    val amount = cursor.getInt(amountIdx)

                    val fine = Fine(LocalDate.parse(receiptDate), description, amount)
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
                val receiptDateIdx = cursor.getColumnIndex(RECEIPT_DATE_COLUMN_NAME)
                val descriptionIdx = cursor.getColumnIndex(DESCRIPTION_COLUMN_NAME)
                val amountIdx = cursor.getColumnIndex(AMOUNT_COLUMN_NAME)

                val receiptDate = cursor.getString(receiptDateIdx)
                val description = cursor.getString(descriptionIdx)
                val amount = cursor.getInt(amountIdx)

                val fine = Fine(LocalDate.parse(receiptDate), description, amount)
                fines.add(fine)
            }
        }
        cursor.close()
        return fines
    }
}