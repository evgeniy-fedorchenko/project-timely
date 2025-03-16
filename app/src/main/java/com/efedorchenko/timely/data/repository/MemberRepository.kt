package com.efedorchenko.timely.data.repository

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.CHANGED_AT_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.ID_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.MEMBERS_TABLE_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.NAME_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.POSITION_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.ROLE_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.SPACE_STATUS_COLUMN_NAME
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.TAG
import com.efedorchenko.timely.data.repository.DatabaseConfigurer.Companion.USER_UUID_COLUMN_NAME
import com.efedorchenko.timely.model.auth.RoleType
import com.efedorchenko.timely.model.member.SpaceMember
import com.efedorchenko.timely.model.member.SpaceStatus
import org.threeten.bp.Instant
import java.sql.SQLException
import javax.inject.Inject

class MemberRepository @Inject constructor(application: Application) {

    companion object {
        private const val SELECT_MEMBERS = """
        SELECT $ID_COLUMN_NAME, $NAME_COLUMN_NAME, $ROLE_COLUMN_NAME, $POSITION_COLUMN_NAME, $SPACE_STATUS_COLUMN_NAME, $USER_UUID_COLUMN_NAME, $CHANGED_AT_COLUMN_NAME 
        FROM $MEMBERS_TABLE_NAME 
        WHERE $SPACE_STATUS_COLUMN_NAME IN (%s)
    """
    }

    private val dbHelper = DatabaseConfigurer.getInstance(application)

    fun save(members: List<SpaceMember>) {
        if (members.isEmpty()) return
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

    fun getMembersList(vararg statuses: SpaceStatus): List<SpaceMember> {
        val db = dbHelper.readableDatabase
        val members = mutableListOf<SpaceMember>()
        var cursor: Cursor? = null
        db.beginTransaction()

        try {
            val placeholders = statuses.joinToString(",") { "?" }
            val query = String.format(SELECT_MEMBERS, placeholders)
            val selectionArgs = statuses.map { it.name }.toTypedArray()

            cursor = db.rawQuery(query, selectionArgs)?.run {
                while (moveToNext()) {
                    val name = columnAs(NAME_COLUMN_NAME) { getString(it) }
                    val role = columnAs(ROLE_COLUMN_NAME) { getString(it) }
                    val position = columnAs(POSITION_COLUMN_NAME) { getString(it) }
                    val userUuid = columnAs(USER_UUID_COLUMN_NAME) { getString(it) }
                    val changedAt = columnAs(CHANGED_AT_COLUMN_NAME) { getLong(it) }
                    val spaceStatus = columnAs(SPACE_STATUS_COLUMN_NAME) { getString(it) }

//                    Колонки not null, но columnAs обязывает
                    if (userUuid != null && name != null && position != null && spaceStatus != null) {
                        val member = SpaceMember(
                            userUuid = userUuid,
                            name = name,
                            role = role?.let { RoleType.valueOf(it) },
                            position = position,
                            changedAt = changedAt?.let { Instant.ofEpochMilli(it) },
                            spaceStatus = SpaceStatus.valueOf(spaceStatus)
                        )
                        members.add(member)
                    }
                }
                this
            }
            db.setTransactionSuccessful()
        } catch (ex: Exception) {
            Log.e(TAG, "Error when extracting members of statuses [$statuses]. Ex: $ex")
        } finally {
            cursor?.close()
            db.endTransaction()
        }
        return members
    }

    fun clean() {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        db.delete(MEMBERS_TABLE_NAME, null, null)
        db.setTransactionSuccessful()
        db.endTransaction()
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

    fun deleteIfNotContains(userIdsToKeep: List<String>) {
        val whereClause = "$USER_UUID_COLUMN_NAME NOT IN (${userIdsToKeep.joinToString { "'$it'" }})"
        dbHelper.writableDatabase.delete(MEMBERS_TABLE_NAME, whereClause, null)
    }

    fun updateStatus(newStatus: SpaceStatus, updatableUserId: String) {
        try {
            val contentValues = ContentValues().apply {
                put(SPACE_STATUS_COLUMN_NAME, newStatus.toString())
            }
            dbHelper.writableDatabase.update(
                MEMBERS_TABLE_NAME,
                contentValues,
                "$USER_UUID_COLUMN_NAME = ?",
                arrayOf(updatableUserId)
            )
        } catch (e: SQLException) {
            Log.e(TAG, "Error updating member status: ${e.message}")
        }
    }

    fun delete(userUuid: String) {
        val whereClause = "$USER_UUID_COLUMN_NAME = ?"
        dbHelper.readableDatabase.delete(MEMBERS_TABLE_NAME, whereClause, arrayOf(userUuid))
    }

    private fun extractContentValues(member: SpaceMember) = ContentValues().apply {
        put(USER_UUID_COLUMN_NAME, member.userUuid)
        put(NAME_COLUMN_NAME, member.name)
        put(ROLE_COLUMN_NAME, member.role.toString())
        put(POSITION_COLUMN_NAME, member.position)
        put(SPACE_STATUS_COLUMN_NAME, member.spaceStatus.toString())
        member.changedAt?.let { put(CHANGED_AT_COLUMN_NAME, it.toEpochMilli()) }
    }

    //    Для отладки
    fun getAll(): List<Map<String, String>> {
        val resultList = mutableListOf<Map<String, String>>()
        val cursor = dbHelper.readableDatabase.rawQuery("SELECT * FROM members", null)
        val columnNames = cursor.columnNames

        while (cursor.moveToNext()) {
            val rowMap = columnNames.mapIndexed { index, columnName ->
                columnName to (cursor.getString(index) ?: "null")
            }.toMap()
            resultList.add(rowMap)
        }

        cursor.close()
        return resultList
    }
}