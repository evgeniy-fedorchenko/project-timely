package com.efedorchenko.timely.data

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.CHANGED_AT_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.MEMBERS_TABLE_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.NAME_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.POSITION_COLUMN_NAME
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.TAG
import com.efedorchenko.timely.data.DatabaseConfigurer.Companion.USER_UUID_COLUMN_NAME
import com.efedorchenko.timely.model.SpaceMember
import org.threeten.bp.Instant
import javax.inject.Inject

class MemberRepository @Inject constructor(application: Application) {

    private val dbHelper = DatabaseConfigurer.getInstance(application)

    fun save(members: List<SpaceMember>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            members.forEach { member ->
                val contentValues = ContentValues().apply {
                    put(NAME_COLUMN_NAME, member.name)
                    put(POSITION_COLUMN_NAME, member.position)
                    put(USER_UUID_COLUMN_NAME, member.userUuid)
                }

                db.insertWithOnConflict(
                    MEMBERS_TABLE_NAME,
                    null,
                    contentValues,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } catch (ex: Exception) {
            Log.e(TAG, "Error when saving members. Cause: ${ex.message}")
        } finally {
            db.endTransaction()
        }
    }

    fun getMembersList(): List<SpaceMember> {
        val members = mutableListOf<SpaceMember>()
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null

        db.beginTransaction()
        try {
            cursor = db.query(
                MEMBERS_TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null
            )

            cursor?.let {
                while (cursor.moveToNext()) {

                    val nameIdx = cursor.getColumnIndex(NAME_COLUMN_NAME)
                    val positionIdx = cursor.getColumnIndex(POSITION_COLUMN_NAME)
                    val userUuidIdx = cursor.getColumnIndex(USER_UUID_COLUMN_NAME)

                    val name = cursor.getString(nameIdx)
                    val position = cursor.getString(positionIdx)
                    val userUuid = cursor.getString(userUuidIdx)

                    val member = SpaceMember(userUuid, name, position)
                    members.add(member)
                }
            }
            db.setTransactionSuccessful()
        } catch (ex: Exception) {
            Log.e(TAG, "Error when extracting members. Cause: :${ex.message}")
        } finally {
            cursor?.close()
            db.endTransaction()
        }

        return members
    }


    fun clean() {
        val db = dbHelper.writableDatabase
        db.delete(MEMBERS_TABLE_NAME, null, null)
    }

    fun getMaxChangedAt(): Instant? {
        val sql = "SELECT MAX($CHANGED_AT_COLUMN_NAME) FROM $MEMBERS_TABLE_NAME"
        return dbHelper.readableDatabase.rawQuery(sql, null)
            .use { cursor ->
                if (cursor.moveToFirst()) {
                    val maxTime = cursor.getLong(0)
                    if (maxTime > 0) Instant.ofEpochMilli(maxTime) else null
                } else null
            }
    }
}