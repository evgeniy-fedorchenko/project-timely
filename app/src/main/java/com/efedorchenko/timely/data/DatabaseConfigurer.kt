package com.efedorchenko.timely.data

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseConfigurer private constructor(application: Application) :
    SQLiteOpenHelper(application, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {

        @Volatile
        private var _instance: DatabaseConfigurer? = null

        fun getInstance(application: Application): DatabaseConfigurer {
            return _instance ?: synchronized(this) {
                _instance ?: DatabaseConfigurer(application).also { _instance = it }
            }
        }

        const val TAG = "DatabaseError"
        private const val DATABASE_NAME = "timely.db"
        private const val DATABASE_VERSION = 6

        const val EVENTS_TABLE_NAME = "events"
        const val FINES_TABLE_NAME = "fines"
        const val MEMBERS_TABLE_NAME = "members"
        private const val EVENTS_MONTH_UID_INDEX_NAME = "events_month_uid_idx"
        private const val FINES_MONTH_UID_INDEX_NAME = "fines_month_uid_idx"
        private const val EVENTS_CHANGED_AT_INDEX_NAME = "events_changed_at_idx"
        private const val FINES_CHANGED_AT_INDEX_NAME = "fines_changed_at_idx"

        const val ID_COLUMN_NAME = "id"                     // Integer primary key autoincrement
        const val BACKEND_ID_COLUMN_NAME = "backend_id"     // Integer unique
        const val DATE_COLUMN_NAME = "date"                 // Text not null (unique for events)
        const val MONTH_UID_COLUMN_NAME = "month_uid_hash"  // Integer not null
        const val CHANGED_AT_COLUMN_NAME = "changed_at"     // Integer

        /* Event */
        const val WORK_MINUTES_COLUMN_NAME = "work_minutes" // Integer not null
        const val COMMENT_COLUMN_NAME = "comment"           // Text

        /* Fine */
        const val DESCRIPTION_COLUMN_NAME = "description"   // Text not null
        const val AMOUNT_COLUMN_NAME = "amount"             // Integer not null

        /* Member */
        const val NAME_COLUMN_NAME = "member_name"          // Text not null
        const val POSITION_COLUMN_NAME = "position"         // Text not null
        const val USER_UUID_COLUMN_NAME = "user_uuid"       // Text not null unique

        private const val EVENTS_CREATE_TABLE =            "CREATE TABLE $EVENTS_TABLE_NAME($ID_COLUMN_NAME INTEGER PRIMARY KEY AUTOINCREMENT, $BACKEND_ID_COLUMN_NAME INTEGER UNIQUE, $MONTH_UID_COLUMN_NAME INTEGER NOT NULL, $DATE_COLUMN_NAME TEXT NOT NULL UNIQUE, $WORK_MINUTES_COLUMN_NAME INTEGER NOT NULL, $COMMENT_COLUMN_NAME TEXT, $CHANGED_AT_COLUMN_NAME INTEGER)"
        private const val EVENTS_CREATE_MONTH_UID_INDEX =  "CREATE INDEX $EVENTS_MONTH_UID_INDEX_NAME ON $EVENTS_TABLE_NAME($MONTH_UID_COLUMN_NAME)"
        private const val EVENTS_CREATE_CHANGED_AT_INDEX = "CREATE INDEX $EVENTS_CHANGED_AT_INDEX_NAME ON $EVENTS_TABLE_NAME($CHANGED_AT_COLUMN_NAME DESC)"
        private const val EVENTS_DROP_TABLE =              "DROP TABLE IF EXISTS $EVENTS_TABLE_NAME"

        private const val FINES_CREATE_TABLE =            "CREATE TABLE $FINES_TABLE_NAME($ID_COLUMN_NAME INTEGER PRIMARY KEY AUTOINCREMENT, $BACKEND_ID_COLUMN_NAME INTEGER UNIQUE, $MONTH_UID_COLUMN_NAME INTEGER NOT NULL, $DATE_COLUMN_NAME TEXT NOT NULL, $DESCRIPTION_COLUMN_NAME TEXT NOT NULL, $AMOUNT_COLUMN_NAME INTEGER NOT NULL, $CHANGED_AT_COLUMN_NAME INTEGER)"
        private const val FINES_CREATE_MONTH_UID_INDEX =  "CREATE INDEX $FINES_MONTH_UID_INDEX_NAME ON $FINES_TABLE_NAME($MONTH_UID_COLUMN_NAME)"
        private const val FINES_CREATE_CHANGED_AT_INDEX = "CREATE INDEX $FINES_CHANGED_AT_INDEX_NAME ON $FINES_TABLE_NAME($CHANGED_AT_COLUMN_NAME DESC)"
        private const val FINES_DROP_TABLE =              "DROP TABLE IF EXISTS $FINES_TABLE_NAME"

        private const val MEMBERS_CREATE_TABLE = "CREATE TABLE $MEMBERS_TABLE_NAME($ID_COLUMN_NAME INTEGER PRIMARY KEY AUTOINCREMENT, $NAME_COLUMN_NAME TEXT NOT NULL, $POSITION_COLUMN_NAME TEXT NOT NULL, $USER_UUID_COLUMN_NAME TEXT NOT NULL UNIQUE, $CHANGED_AT_COLUMN_NAME INTEGER)"
        private const val MEMBERS_DROP_TABLE =   "DROP TABLE IF EXISTS $MEMBERS_TABLE_NAME"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.let {
            try {
                it.beginTransaction()

                it.execSQL(EVENTS_CREATE_TABLE)
                it.execSQL(EVENTS_CREATE_MONTH_UID_INDEX)
                it.execSQL(EVENTS_CREATE_CHANGED_AT_INDEX)

                it.execSQL(FINES_CREATE_TABLE)
                it.execSQL(FINES_CREATE_MONTH_UID_INDEX)
                it.execSQL(FINES_CREATE_CHANGED_AT_INDEX)

                it.execSQL(MEMBERS_CREATE_TABLE)

                it.setTransactionSuccessful()
            } catch (ex: Exception) {
                Log.e(TAG, "Error when creating a table or index. Cause: ${ex.message}")
            } finally {
                it.endTransaction()
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(EVENTS_DROP_TABLE)
        db?.execSQL(FINES_DROP_TABLE)
        db?.execSQL(MEMBERS_DROP_TABLE)
        onCreate(db)
    }
}
