package com.efedorchenko.timely.data

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.core.content.contentValuesOf
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.BACKEND_ID_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.COMMENT_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.EVENTS_TABLE_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.EVENT_DATE_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.ID_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.MONTH_UID_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.TAG
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.WORK_MINUTES_COLUMN_NAME
import com.efedorchenko.timely.model.Event
import com.efedorchenko.timely.model.MonthUID
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import javax.inject.Inject

class EventRepository @Inject constructor(application: Application) : DataRepository<Event> {

    private val dbHelper = DatabaseConfigurer.getInstance(application)

    override fun save(data: Event): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(MONTH_UID_COLUMN_NAME, MonthUID.create(data.date).value)
            put(EVENT_DATE_COLUMN_NAME, data.date.toString())
            put(WORK_MINUTES_COLUMN_NAME, data.workDuration.toMinutes().toInt())
            put(COMMENT_COLUMN_NAME, data.comment)
        }
        val id = db.insert(EVENTS_TABLE_NAME, null, values)
        if (id == -1L) {
            Log.e(TAG, "Error when insert event [$data]")
        }
        return id
    }

    override fun saveBatch(dataBatch: List<Event>) {
        val db = dbHelper.writableDatabase

        db.beginTransaction()
        try {
            dataBatch.forEach { data ->
                val values = ContentValues().apply {
                    put(BACKEND_ID_COLUMN_NAME, data.backendId)
                    put(MONTH_UID_COLUMN_NAME, MonthUID.create(data.date).value)
                    put(EVENT_DATE_COLUMN_NAME, data.date.toString())
                    put(WORK_MINUTES_COLUMN_NAME, data.workDuration.toMinutes().toInt())
                    put(COMMENT_COLUMN_NAME, data.comment)
                }

                val id = db.insert(EVENTS_TABLE_NAME, null, values)
                if (id == -1L) {
                    Log.e(TAG, "Error when insert event from batch: $data")
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun upsert(data: Event): Long {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(ID_COLUMN_NAME, data.appId)
            put(MONTH_UID_COLUMN_NAME, MonthUID.create(data.date).value)
            put(EVENT_DATE_COLUMN_NAME, data.date.toString())
            put(WORK_MINUTES_COLUMN_NAME, data.workDuration.toMinutes().toInt())
            put(COMMENT_COLUMN_NAME, data.comment)
            data.backendId?.let {
                put(BACKEND_ID_COLUMN_NAME, it)
            }
        }

        return db.insertWithOnConflict(
            EVENTS_TABLE_NAME,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        ).also { id ->
            if (id == -1L) {
                Log.e(TAG, "Error when upsetting event [$data]")
            }
        }
    }

    override fun findByMonth(monthUID: MonthUID, withComment: Boolean): List<Event> {
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
                    val idIndex = cursor.getColumnIndex(ID_COLUMN_NAME)
                    val backendIdIndex = cursor.getColumnIndex(BACKEND_ID_COLUMN_NAME)
                    val eventDateIdx = cursor.getColumnIndex(EVENT_DATE_COLUMN_NAME)
                    val workMinutesIdx = cursor.getColumnIndex(WORK_MINUTES_COLUMN_NAME)

                    val id = cursor.getLong(idIndex)
                    val backendId = cursor.getLong(backendIdIndex)
                    val eventDate = cursor.getString(eventDateIdx)
                    val workMinutes = cursor.getLong(workMinutesIdx)
                    var comment: String? = null

                    if (withComment) {
                        val commentIdx = cursor.getColumnIndex(COMMENT_COLUMN_NAME)
                        comment = cursor.getString(commentIdx)
                    }

                    val event = Event(
                        appId = id,
                        backendId = backendId,
                        date = LocalDate.parse(eventDate),
                        workDuration = Duration.ofMinutes(workMinutes),
                        comment = comment
                    )
                    events.add(event)
                }
            }
            db.setTransactionSuccessful()
        } catch (ex: Exception) {
            Log.e(TAG, "Error when extracting events. Cause: :${ex.message}")
        } finally {
            cursor?.close()
            db.endTransaction()
        }

        return events
    }

    override fun setBackendId(data: Event) {
        val db = dbHelper.writableDatabase
        db.update(
            EVENTS_TABLE_NAME,
            contentValuesOf(Pair(BACKEND_ID_COLUMN_NAME, data.backendId)),
            "$ID_COLUMN_NAME = ?",
            arrayOf(data.appId.toString())
        )
    }

    override fun findNullableBackendId(): List<Event> {
        val db = dbHelper.readableDatabase
        val events = mutableListOf<Event>()
        var cursor: Cursor? = null
        db.beginTransaction()

        try {
            cursor = db.query(
                EVENTS_TABLE_NAME,
                null,
                "$BACKEND_ID_COLUMN_NAME IS NULL",
                null,
                null,
                null,
                null
            )
            cursor?.let {
                while (cursor.moveToNext()) {
                    val idIndex = cursor.getColumnIndex(ID_COLUMN_NAME)
                    val eventDateIdx = cursor.getColumnIndex(EVENT_DATE_COLUMN_NAME)
                    val workMinutesIdx = cursor.getColumnIndex(WORK_MINUTES_COLUMN_NAME)
                    val commentIdx = cursor.getColumnIndex(COMMENT_COLUMN_NAME)

                    val id = cursor.getLong(idIndex)
                    val eventDate = cursor.getString(eventDateIdx)
                    val workMinutes = cursor.getLong(workMinutesIdx)
                    val comment = cursor.getString(commentIdx)

                    val event = Event(
                        appId = id,
                        date = LocalDate.parse(eventDate),
                        workDuration = Duration.ofMinutes(workMinutes),
                        comment = comment
                    )
                    events.add(event)
                }
            }
            db.setTransactionSuccessful()
        } catch (ex: Exception) {
            Log.e(TAG, "Error when extracting events with nullable backendId. Cause: :${ex.message}")
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
            Log.e(TAG, "No event was deleted with id: $id")
            return false
        }
    }

    override fun clean() {
        val db = dbHelper.writableDatabase
        db.delete(EVENTS_TABLE_NAME, null, null)
    }
}