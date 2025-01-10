package com.efedorchenko.timely.data

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.core.content.contentValuesOf
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
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import javax.inject.Inject

class EventRepository @Inject constructor(application: Application) : DataRepository<Event> {

    companion object {
        private const val SELECT_EVENTS_BY_MONTH_UID =         "SELECT * FROM $EVENTS_TABLE_NAME WHERE $MONTH_UID_COLUMN_NAME = ?"
        private const val SELECT_EVENTS_WITH_NULL_BACKEND_ID = "SELECT * FROM $EVENTS_TABLE_NAME WHERE $BACKEND_ID_COLUMN_NAME IS NULL"
        private const val SELECT_MAX_CHANGED_AT =              "SELECT MAX($CHANGED_AT_COLUMN_NAME) FROM $EVENTS_TABLE_NAME"
    }

    private val dbHelper = DatabaseConfigurer.getInstance(application)

    override fun save(data: Event): Long = saveOne(dbHelper.writableDatabase, data, SQLiteDatabase.CONFLICT_NONE)

    override fun upsert(data: Event): Long {
        return if (data.deletedAt != null) {
            if (data.appId?.let { deleteById(it) } == true) {
                data.appId ?: -1
            } else -1
        } else {
            saveOne(dbHelper.writableDatabase, data, SQLiteDatabase.CONFLICT_NONE)
        }
    }

    override fun saveBatch(dataBatch: List<Event>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            dataBatch.forEach { saveOne(db, it, SQLiteDatabase.CONFLICT_NONE) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun upsertBatch(dataBatch: List<Event>) {
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

    override fun setBackendProperties(data: Event) {
        dbHelper.writableDatabase.update(
            EVENTS_TABLE_NAME,
            contentValuesOf(Pair(CHANGED_AT_COLUMN_NAME, data.changedAt), Pair(BACKEND_ID_COLUMN_NAME, data.backendId)),
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
        val deletedRows = db.delete(EVENTS_TABLE_NAME, "$ID_COLUMN_NAME = ?", arrayOf(id.toString()))

        if (deletedRows == 0) {
            return true
        } else {
            Log.e(TAG, "No event was deleted with id: $id. Ex: ")
            return false
        }
    }

    override fun clean() {
        dbHelper.writableDatabase.delete(EVENTS_TABLE_NAME, null, null)
    }

    private fun saveOne(writableDb: SQLiteDatabase, event: Event, conflictAlgorithm: Int): Long {
        try {
            val values = extractContentValues(event)
            return writableDb.insertWithOnConflict(EVENTS_TABLE_NAME, null, values, conflictAlgorithm)
        } catch (ex: SQLException) {
            Log.e(TAG, "Error when insert event with conflict algorithm [$conflictAlgorithm] event: $event. Ex: $ex")
            return -1
        }
    }

    private fun extractContentValues(data: Event): ContentValues {
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
