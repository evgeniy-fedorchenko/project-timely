package com.efedorchenko.timely.repository

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseConfigurer private constructor(private val application: Application) :
    SQLiteOpenHelper(application, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {

        @Volatile
        private var _instance: DatabaseConfigurer? = null

        fun getInstance(application: Application): DatabaseConfigurer {
            return _instance ?: synchronized(this) {
                _instance ?: DatabaseConfigurer(application).also { _instance = it }
            }
        }

        private const val DATABASE_NAME = "timely.db"
        private const val DATABASE_VERSION = 1
        private const val EVENTS_INDEX_NAME = "events_month_uid_idx"
        private const val FINES_INDEX_NAME = "fines_month_uid_idx"
        const val EVENTS_TABLE_NAME = "events"
        const val FINES_TABLE_NAME = "fines"

        const val ID_COLUMN_NAME = "id"
        const val MONTH_UID_COLUMN_NAME = "month_uid_hash"

        const val EVENT_DATE_COLUMN_NAME = "event_date"
        const val WORK_MINUTES_COLUMN_NAME = "work_minutes"
        const val COMMENT_COLUMN_NAME = "comment"

        const val RECEIPT_DATE_COLUMN_NAME = "receipt_date"
        const val DESCRIPTION_COLUMN_NAME = "description"
        const val AMOUNT_COLUMN_NAME = "amount"

        private const val EVENTS_CREATE_TABLE = "CREATE TABLE $EVENTS_TABLE_NAME($ID_COLUMN_NAME INTEGER PRIMARY KEY AUTOINCREMENT, $MONTH_UID_COLUMN_NAME INTEGER NOT NULL, $EVENT_DATE_COLUMN_NAME TEXT NOT NULL, $WORK_MINUTES_COLUMN_NAME INTEGER NOT NULL, $COMMENT_COLUMN_NAME TEXT)"
        private const val EVENTS_CREATE_INDEX = "CREATE INDEX $EVENTS_INDEX_NAME ON $EVENTS_TABLE_NAME($MONTH_UID_COLUMN_NAME)"
        private const val EVENTS_DROP_TABLE = "DROP TABLE IF EXISTS $EVENTS_TABLE_NAME"

        private const val FINES_CREATE_TABLE = "CREATE TABLE $FINES_TABLE_NAME($ID_COLUMN_NAME INTEGER PRIMARY KEY AUTOINCREMENT, $MONTH_UID_COLUMN_NAME INTEGER NOT NULL, $RECEIPT_DATE_COLUMN_NAME TEXT NOT NULL, $DESCRIPTION_COLUMN_NAME TEXT NOT NULL, $AMOUNT_COLUMN_NAME INTEGER NOT NULL)"
        private const val FINES_CREATE_INDEX = "CREATE INDEX $FINES_INDEX_NAME ON $FINES_TABLE_NAME($MONTH_UID_COLUMN_NAME)"
        private const val FINES_DROP_TABLE = "DROP TABLE IF EXISTS $FINES_TABLE_NAME"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.let {
            try {
                it.beginTransaction()
                it.execSQL(EVENTS_CREATE_TABLE)
                it.execSQL(EVENTS_CREATE_INDEX)
                it.execSQL(FINES_CREATE_TABLE)
                it.execSQL(FINES_CREATE_INDEX)
                it.setTransactionSuccessful()
            } catch (ex: Exception) {
                Log.e("DatabaseError", "Error when creating a table or index. Cause: ${ex.message}")
            } finally {
                it.endTransaction()
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(EVENTS_DROP_TABLE)
        db?.execSQL(FINES_DROP_TABLE)
        onCreate(db)
    }
}
