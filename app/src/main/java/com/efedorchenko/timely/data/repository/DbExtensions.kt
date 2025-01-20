package com.efedorchenko.timely.data.repository

import android.database.Cursor

fun <T> Cursor.columnAs(columnName: String, func: (Int) -> T): T? {
    return getColumnIndex(columnName).takeIf { it != -1 }?.let { func(it) }
}
