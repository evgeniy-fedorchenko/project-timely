package com.efedorchenko.timely.data.repository

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.CHANGED_AT_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.MEMBERS_TABLE_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.NAME_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.POSITION_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.TAG
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.USER_UUID_COLUMN_NAME
import com.efedorchenko.timely.model.SpaceMember
import org.threeten.bp.Instant
import javax.inject.Inject

class MemberRepository @Inject constructor(application: Application) {

    companion object {
        private const val SELECT_ALL_MEMBERS =
            "SELECT id, member_name, position, user_uuid, changed_at FROM $MEMBERS_TABLE_NAME"
    }

    private val dbHelper = DatabaseConfigurer.getInstance(application)

    fun save(members: List<SpaceMember>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            members.forEach { member ->
                db.insertWithOnConflict(
                    MEMBERS_TABLE_NAME,
                    null,
                    extractContentValues(member),
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
        val db = dbHelper.readableDatabase
        val members = mutableListOf<SpaceMember>()
        var cursor: Cursor? = null
        db.beginTransaction()

        try {
            cursor = db.rawQuery(SELECT_ALL_MEMBERS, null)?.run {
                while (moveToNext()) {
                    val name = columnAs(NAME_COLUMN_NAME) { getString(it) }
                    val position = columnAs(POSITION_COLUMN_NAME) { getString(it) }
                    val userUuid = columnAs(USER_UUID_COLUMN_NAME) { getString(it) }
                    val changedAt = columnAs(CHANGED_AT_COLUMN_NAME) { getLong(it) }

                    if (userUuid != null && name != null && position != null) {
                        val member = SpaceMember(
                            userUuid = userUuid,
                            name = name,
                            position = position,
                            changedAt = changedAt?.let { Instant.ofEpochMilli(changedAt) }
                        )
                        members.add(member)
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
        return members
    }

    fun clean() {
        dbHelper.writableDatabase.delete(MEMBERS_TABLE_NAME, null, null)
    }

    // TODO: Проверить, почему-то возвращает не то что нужно, в ответе в этим сайнсом возвращаются все мемберы
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

    fun deleteIfNotContains(userIdsToKeep: List<String>) {
        val whereClause = "$USER_UUID_COLUMN_NAME NOT IN (${userIdsToKeep.joinToString { "'$it'" }})"
        dbHelper.readableDatabase.delete(MEMBERS_TABLE_NAME, whereClause, null)
    }

    private fun extractContentValues(member: SpaceMember) = ContentValues().apply {
        put(USER_UUID_COLUMN_NAME, member.userUuid)
        put(NAME_COLUMN_NAME, member.name)
        put(POSITION_COLUMN_NAME, member.position)
        member.changedAt?.let { put(CHANGED_AT_COLUMN_NAME, it.toEpochMilli()) }
    }
}