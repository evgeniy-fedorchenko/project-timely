package com.efedorchenko.timely.data

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.BACKEND_ID_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.CHANGED_AT_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.COMMENT_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.DATE_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.EVENTS_TABLE_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.ID_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.MONTH_UID_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.TAG
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.WORK_MINUTES_COLUMN_NAME
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.MonthUID
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import javax.inject.Inject

class EventRepository @Inject constructor(application: Application) : DataRepository<Event>(application) {

    companion object {
        private const val SELECT_EVENTS_BY_MONTH_UID =         "SELECT * FROM $EVENTS_TABLE_NAME WHERE $MONTH_UID_COLUMN_NAME = ?"
        private const val SELECT_EVENTS_WITH_NULL_BACKEND_ID = "SELECT * FROM $EVENTS_TABLE_NAME WHERE $BACKEND_ID_COLUMN_NAME IS NULL"
    }

    private val dbHelper = DatabaseConfigurer.getInstance(application)

    /**
     * Возвращается без `backend_id` и `changed_at``
     */
    override fun findByMonth(monthUID: MonthUID, withComment: Boolean): List<Event> {
        val db = dbHelper.readableDatabase
        val events = mutableListOf<Event>()
        var cursor: Cursor? = null
        db.beginTransaction()

        try {
            cursor = db.rawQuery(SELECT_EVENTS_BY_MONTH_UID, arrayOf(monthUID.hashCode().toString()))
                ?.run {
                    while (moveToNext()) {
                        val id = columnAs(ID_COLUMN_NAME) { idx -> getLong(idx) }
                        val date = columnAs(DATE_COLUMN_NAME) { idx -> getString(idx) }
                        val workMinutes = columnAs(WORK_MINUTES_COLUMN_NAME) { idx -> getLong(idx) }

                        val comment = if (withComment) {
                            columnAs(COMMENT_COLUMN_NAME) { idx -> getString(idx) }
                        } else null

                        if (date != null && workMinutes != null) {
                            val event = Event(
                                appId = id,
                                date = LocalDate.parse(date),
                                workDuration = Duration.ofMinutes(workMinutes),
                                comment = comment,
                            )
                            events.add(event)
                        }
                    }
                    this
                }
            db.setTransactionSuccessful()
        } catch (ex: Exception) {
            Log.e(TAG, "Error when finding events by month uid. Ex :$ex")
        } finally {
            cursor?.close()
            db.endTransaction()
        }
        return events
    }

    override fun findNullableBackendId(): List<Event> {
        val db = dbHelper.readableDatabase
        val events = mutableListOf<Event>()
        var cursor: Cursor? = null
        db.beginTransaction()

        try {
            cursor = db.rawQuery(SELECT_EVENTS_WITH_NULL_BACKEND_ID, null)
                ?.run {
                    while (moveToNext()) {
                        val id = columnAs(ID_COLUMN_NAME) { idx -> getLong(idx) }
                        val date = columnAs(DATE_COLUMN_NAME) { idx -> getString(idx) }
                        val workMinutes = columnAs(WORK_MINUTES_COLUMN_NAME) { idx -> getLong(idx) }
                        val comment = columnAs(COMMENT_COLUMN_NAME) { idx -> getString(idx) }

                        if (date != null && workMinutes != null) {
                            val event = Event(
                                appId = id,
                                date = LocalDate.parse(date),
                                workDuration = Duration.ofMinutes(workMinutes),
                                comment = comment,
                            )
                            events.add(event)
                        }
                    }
                    this
                }
            db.setTransactionSuccessful()
        } catch (ex: Exception) {
            Log.e(TAG, "Error when extracting events with nullable backendId. Ex: $ex")
        } finally {
            cursor?.close()
            db.endTransaction()
        }
        return events
    }

    override fun getTableName() = EVENTS_TABLE_NAME

    override fun extractContentValues(data: Event): ContentValues {
        return ContentValues().apply {
            data.appId?.let { put(ID_COLUMN_NAME, data.appId) }
            data.backendId?.let { put(BACKEND_ID_COLUMN_NAME, it) }
            put(DATE_COLUMN_NAME, data.date.toString())
            put(MONTH_UID_COLUMN_NAME, MonthUID.create(data.date).value)
            data.changedAt?.let { put(CHANGED_AT_COLUMN_NAME, it.toEpochMilli()) }

            put(WORK_MINUTES_COLUMN_NAME, data.workDuration.toMinutes().toInt())
            data.comment?.let { put(COMMENT_COLUMN_NAME, it) }
        }
    }
}
