package com.efedorchenko.timely.repository

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.efedorchenko.timely.data.Event
import com.efedorchenko.timely.data.MonthUID
import com.efedorchenko.timely.repository.DatabaseConfigurer.Companion.COMMENT_COLUMN_NAME
import com.efedorchenko.timely.repository.DatabaseConfigurer.Companion.EVENTS_TABLE_NAME
import com.efedorchenko.timely.repository.DatabaseConfigurer.Companion.EVENT_DATE_COLUMN_NAME
import com.efedorchenko.timely.repository.DatabaseConfigurer.Companion.MONTH_UID_COLUMN_NAME
import com.efedorchenko.timely.repository.DatabaseConfigurer.Companion.WORK_MINUTES_COLUMN_NAME
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate

class EventRepository(private val application: Application) {

    private val dbHelper = DatabaseConfigurer.getInstance(application)

    fun save(event: Event) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(MONTH_UID_COLUMN_NAME, MonthUID.create(event.eventDate).hashCode())
            put(EVENT_DATE_COLUMN_NAME, event.eventDate.toString())
            put(WORK_MINUTES_COLUMN_NAME, event.workDuration.toMinutes().toInt())
            put(COMMENT_COLUMN_NAME, event.comment)
        }
        db.insert(EVENTS_TABLE_NAME, null, values)
    }

    fun findByMonth(monthUID: MonthUID, withComment: Boolean): MutableList<Event> {
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
}