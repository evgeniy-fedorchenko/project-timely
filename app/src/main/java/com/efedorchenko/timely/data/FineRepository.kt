package com.efedorchenko.timely.data

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.AMOUNT_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.BACKEND_ID_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.DESCRIPTION_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.FINES_TABLE_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.ID_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.MONTH_UID_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.RECEIPT_DATE_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.TAG
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.model.MonthUID
import org.threeten.bp.LocalDate
import javax.inject.Inject

class FineRepository @Inject constructor(application: Application): DataRepository<Fine> {

    private val dbHelper = DatabaseConfigurer.getInstance(application)

    override fun save(data: Fine): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(MONTH_UID_COLUMN_NAME, MonthUID.create(data.receiptDate).hashCode())
            put(RECEIPT_DATE_COLUMN_NAME, data.receiptDate.toString())
            put(DESCRIPTION_COLUMN_NAME, data.description)
            put(AMOUNT_COLUMN_NAME, data.amount)
        }

        val id = db.insert(FINES_TABLE_NAME, null, values)
        if (id == -1L) {
            Log.e(TAG, "Error when insert fine $data")
        }
        return id
    }

    override fun findByMonth(monthUID: MonthUID, withComment: Boolean): MutableList<Fine> {
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
            Log.e(TAG, "Error when extracting events. Cause: :${ex.message}")
        } finally {
            cursor?.close()
            db.endTransaction()
        }

        return fines
    }

    // FIXME
   /* fun getAllFines(): List<Fine> {
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
            while (moveToNext()) {
                val idIdx = getColumnIndex(ID_COLUMN_NAME)
                val receiptDateIdx = getColumnIndex(RECEIPT_DATE_COLUMN_NAME)
                val descriptionIdx = getColumnIndex(DESCRIPTION_COLUMN_NAME)
                val amountIdx = getColumnIndex(AMOUNT_COLUMN_NAME)

                val id = getLong(idIdx)
                val receiptDate = getString(receiptDateIdx)
                val description = getString(descriptionIdx)
                val amount = getInt(amountIdx)

                val fine = Fine(id, LocalDate.parse(receiptDate), description, amount)
                fines.add(fine)
            }
        }
        cursor.close()
        return fines
    }
*/
    override fun deleteById(id: Long?): Boolean {
        val db = dbHelper.writableDatabase

        val deletedRows = db.delete(
            FINES_TABLE_NAME,
            "$ID_COLUMN_NAME = ?",
            arrayOf(id.toString())
        )

        if (deletedRows > 0) {
            return true
        } else {
            Log.e(TAG, "No fine was deleted with id: $id")
            return false
        }
    }

    override fun setBackendId(data: Fine) {
        TODO("Not yet implemented")
    }
}