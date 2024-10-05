package com.efedorchenko.timely.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "timely.db"
        private const val DATABASE_VERSION = 1
        private const val INDEX_NAME = "monthUIDIdx"
        private const val TABLE_NAME = "events"

        private const val COLUMN_ID = "id"
        private const val COLUMN_MONTH_UID = "month_uid"
        private const val COLUMN_EVENT_DATE = "event_date"
        private const val COLUMN_WORK_MINUTES = "work_minutes"
        private const val COLUMN_COMMENT = "comment"

        private const val CREATE_TABLE = "CREATE TABLE $TABLE_NAME($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_MONTH_UID INTEGER NOT NULL, $COLUMN_EVENT_DATE TEXT NOT NULL, $COLUMN_WORK_MINUTES INTEGER NOT NULL, $COLUMN_COMMENT TEXT)"
        private const val CREATE_INDEX = "CREATE INDEX $INDEX_NAME on $TABLE_NAME($COLUMN_MONTH_UID)"
        private const val DROP_TABLE = "DROP TABLE IF EXISTS $TABLE_NAME"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.let {
            try {
                it.beginTransaction()
                it.execSQL(CREATE_TABLE)
                it.execSQL(CREATE_INDEX)
                it.setTransactionSuccessful()
            } catch (ex: Exception) {
                Log.e("DatabaseError", "Error when creating a table or index. Cause: ${ex.message}")
            } finally {
                it.endTransaction()
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(DROP_TABLE)
        onCreate(db)
    }

    fun save(event: Event) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_MONTH_UID, genMonthUID(event.eventDate))
            put(COLUMN_EVENT_DATE, event.eventDate.toString())
            put(COLUMN_WORK_MINUTES, event.workMinutes.toMinutes().toInt())
            put(COLUMN_COMMENT, event.comment)
        }
        db.insert(TABLE_NAME, null, values)
    }

    fun findByMonth(date: LocalDate, withComment: Boolean): MutableList<Event> {
        val monthUID = genMonthUID(date)
        val events = mutableListOf<Event>()
        val db = this.readableDatabase
        var cursor: Cursor? = null

        db.beginTransaction()
        try {
            cursor = db.query(
                TABLE_NAME,
                null,
                "$COLUMN_MONTH_UID = ?",
                arrayOf(monthUID.toString()),
                null,
                null,
                null
            )

            cursor?.let {
                while (cursor.moveToNext()) {
                    val eventDateIdx = cursor.getColumnIndex(COLUMN_EVENT_DATE)
                    val workMinutesIdx = cursor.getColumnIndex(COLUMN_WORK_MINUTES)

                    val eventDate = cursor.getString(eventDateIdx)
                    val workMinutes = cursor.getInt(workMinutesIdx)
                    var comment: String? = null

                    if (withComment) {
                        val commentIdx = cursor.getColumnIndex(COLUMN_COMMENT)
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

    private fun genMonthUID(date: LocalDate): Int = date.year * 100 + date.monthValue

}
