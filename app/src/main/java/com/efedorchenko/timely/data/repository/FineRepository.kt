package com.efedorchenko.timely.data.repository

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.AMOUNT_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.BACKEND_ID_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.CHANGED_AT_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.COMMENT_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.DATE_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.DESCRIPTION_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.FINES_TABLE_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.ID_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.MEMBERS_FINES_TABLE_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.MONTH_UID_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.TAG
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.USER_UUID_COLUMN_NAME
import com.efedorchenko.timely.model.Fine
import com.efedorchenko.timely.model.MonthUID
import org.threeten.bp.LocalDate
import javax.inject.Inject

class FineRepository @Inject constructor(application: Application) : DataRepository<Fine>(application) {

    companion object {
        private const val SELECT_FINES_WITH_NULL_BACKEND_ID = "SELECT * FROM $FINES_TABLE_NAME WHERE $BACKEND_ID_COLUMN_NAME IS NULL"
    }

    private val dbHelper = DatabaseConfigurer.getInstance(application)

    /**
     * Возвращается без `backend_id` и `changed_at`
     * @param withComment - игнорироуется, объекты всегда возвращаются с описанием
     */
    override fun findByMonth(monthUID: MonthUID, withComment: Boolean, userUuid: String?): List<Fine> {
        val db = dbHelper.readableDatabase
        val fines = mutableListOf<Fine>()
        var cursor: Cursor? = null
        db.beginTransaction()

        try {
//            SELECT FROM table_name WHERE month_uid = ? (AND user_uuid = ?)
            val sql = "SELECT * FROM ${getTableName(userUuid != null)} WHERE $MONTH_UID_COLUMN_NAME = ?${(userUuid?.let { " AND $USER_UUID_COLUMN_NAME = ?" } ?: "")}"
            val argsList = mutableListOf(monthUID.value.toString())
            userUuid?.let { argsList.add(userUuid) }
            cursor = db.rawQuery(sql, argsList.toTypedArray())
                ?.run {
                    while (moveToNext()) {
                        val id = columnAs(ID_COLUMN_NAME) { idx -> getLong(idx) }
                        val date = columnAs(DATE_COLUMN_NAME) { idx -> getString(idx) }
                        val amount = columnAs(AMOUNT_COLUMN_NAME) { idx -> getInt(idx) }
                        val description = columnAs(DESCRIPTION_COLUMN_NAME) { idx -> getString(idx) }

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

    override fun getTableName(forMembersData: Boolean): String {
        return if (forMembersData) MEMBERS_FINES_TABLE_NAME else FINES_TABLE_NAME
    }

    override fun extractContentValues(data: Fine, userUuid: String?): ContentValues {
        return ContentValues().apply {
            data.appId?.let { put(ID_COLUMN_NAME, data.appId) }
            data.backendId?.let { put(BACKEND_ID_COLUMN_NAME, it) }
            userUuid?.let { put(USER_UUID_COLUMN_NAME, it) }
            put(DATE_COLUMN_NAME, data.date.toString())
            put(MONTH_UID_COLUMN_NAME, MonthUID.create(data.date).value)
            data.changedAt?.let { put(CHANGED_AT_COLUMN_NAME, it.toEpochMilli()) }

            put(DESCRIPTION_COLUMN_NAME, data.description)
            put(AMOUNT_COLUMN_NAME, data.amount)
        }
    }
}
