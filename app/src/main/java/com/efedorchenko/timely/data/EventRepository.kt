package com.efedorchenko.timely.data

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.COMMENT_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.EVENTS_TABLE_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.EVENT_DATE_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.ID_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.MONTH_UID_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.WORK_MINUTES_COLUMN_NAME
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.MonthUID
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import javax.inject.Inject

class EventRepository @Inject constructor(application: Application): DataRepository<Event> {

    private val dbHelper = DatabaseConfigurer.getInstance(application)

    override fun save(vararg data: Event) {
        if (data.isNotEmpty()) {
            data.forEach { save(it) }
        }
    }

    override fun save(data: Event): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(MONTH_UID_COLUMN_NAME, MonthUID.create(data.eventDate).hashCode())
            put(EVENT_DATE_COLUMN_NAME, data.eventDate.toString())
            put(WORK_MINUTES_COLUMN_NAME, data.workDuration.toMinutes().toInt())
            put(COMMENT_COLUMN_NAME, data.comment)
        }
        val id = db.insert(EVENTS_TABLE_NAME, null, values)
        if (id == -1L) {
            Log.e("InsertError", "Error when insert event $data")
        }
        return id
    }

    override fun findByMonth(monthUID: MonthUID, withComment: Boolean): MutableList<Event> {
        val events = mutableListOf<Event>()
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null

        db.beginTransaction()
        try {
            cursor = db.query(
                EVENTS_TABLE_NAME,
                null,
                "$MONTH_UID_COLUMN_NAME = ?",
                arrayOf(monthUID.hashCode().toString()),
                null,
                null,
                null
            )

            cursor?.let {
                while (cursor.moveToNext()) {
                    val eventDateIdx = cursor.getColumnIndex(EVENT_DATE_COLUMN_NAME)
                    val workMinutesIdx = cursor.getColumnIndex(WORK_MINUTES_COLUMN_NAME)

                    val eventDate = cursor.getString(eventDateIdx)
                    val workMinutes = cursor.getInt(workMinutesIdx)
                    var comment: String? = null

                    if (withComment) {
                        val commentIdx = cursor.getColumnIndex(COMMENT_COLUMN_NAME)
                        comment = cursor.getString(commentIdx)
                    }

                    val event = Event(
                        LocalDate.parse(eventDate),
                        Duration.ofMinutes(workMinutes.toLong()),
                        comment
                    )
                    events.add(event)
                }
            }
            db.setTransactionSuccessful()
        } catch (ex: Exception) {
            Log.e("DatabaseError", "Error when extracting events. Cause: :${ex.message}")
        } finally {
            cursor?.close()
            db.endTransaction()
        }

        return events
    }

    override fun deleteById(id: Long?): Boolean {
        val db = dbHelper.writableDatabase

        val deletedRows = db.delete(
            EVENTS_TABLE_NAME,
            "$ID_COLUMN_NAME = ?",
            arrayOf(id.toString())
        )

        if (deletedRows > 0) {
            return true
        } else {
            Log.e("DeleteError", "No event was deleted with id: $id")
            return false
        }
    }
}