package com.efedorchenko.timely.data.repository

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.AMOUNT_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.BACKEND_ID_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.CHANGED_AT_COLUMN_NAME
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

    private val dbHelper = DatabaseConfigurer.getInstance(application)

    /**
     * Возвращается `id`, `backendId`, `date`, `amount`, `description`.
     *
     * При передаче `userId` будет выполнен поиск ивентов указанного юзера в их таблице.
     * При передаче `null` выполниться поиск собственных ивентов в своей таблице
     *
     * @param withComment - игнорироуется, объекты всегда возвращаются с описанием
     */
    override fun findByMonth(monthUID: MonthUID, withComment: Boolean, userUuid: String?): List<Fine> {
        val db = dbHelper.readableDatabase
        val fines = mutableListOf<Fine>()
        var cursor: Cursor? = null
        db.beginTransaction()

        try {
//            "SELECT FROM table_name WHERE month_uid = ?", optionally with "AND user_uuid = ?"
            val where = "$MONTH_UID_COLUMN_NAME = ?${userUuid?.let { " AND $USER_UUID_COLUMN_NAME = ?" } ?: ""}"
            val sql = "SELECT * FROM ${getTableName(userUuid != null)} WHERE $where"
            val argsList = mutableListOf(monthUID.value.toString())
            userUuid?.let { argsList.add(userUuid) }

            cursor = db.rawQuery(sql, argsList.toTypedArray())?.run {
                while (moveToNext()) {
                    val id = columnAs(ID_COLUMN_NAME) { getLong(it) }
                    val backendId = columnAs(BACKEND_ID_COLUMN_NAME) { getLong(it) }
                    val date = columnAs(DATE_COLUMN_NAME) { getString(it) }
                    val amount = columnAs(AMOUNT_COLUMN_NAME) { getInt(it) }
                    val description = columnAs(DESCRIPTION_COLUMN_NAME) { getString(it) }

//                    Колонки not null, но columnAs обязывает
                    if (amount != null && description != null) {
                        val fine = Fine(
                            appId = id,
                            backendId = backendId,
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

    override fun doFindData(cursor: Cursor?, isUserEvents: Boolean): List<Fine> {
        val fines = mutableListOf<Fine>()
        cursor?.run {
            while (moveToNext()) {
                val id = columnAs(ID_COLUMN_NAME) { getLong(it) }
                val date = columnAs(DATE_COLUMN_NAME) { getString(it) }
                val amount = columnAs(AMOUNT_COLUMN_NAME) { getInt(it) }
                val description = columnAs(DESCRIPTION_COLUMN_NAME) { getString(it) }

                if (amount != null && description != null) {
                    val fine = Fine(
                        appId = id,
                        date = LocalDate.parse(date),
                        amount = amount,
                        description = description,
                        owner = if (isUserEvents) columnAs(USER_UUID_COLUMN_NAME) { getString(it) } else null
                    )
                    fines.add(fine)
                }
            }
        }
        return fines
    }

    override fun getTableName(forMembersData: Boolean): String {
        return if (forMembersData) MEMBERS_FINES_TABLE_NAME else FINES_TABLE_NAME
    }

    override fun extractContentValues(data: Fine, userUuid: String?) = ContentValues().apply {
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
