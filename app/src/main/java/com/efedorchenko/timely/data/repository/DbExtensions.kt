package com.efedorchenko.timely.data.repository

import android.database.Cursor

/**
 * Если колонка существует - применить переданную функцию к ячейке, на которую указывает
 * курсор и вернуть полученное значение. Если колонка не существует - возвращается `null`
 */
fun <T> Cursor.columnAs(columnName: String, func: (Int) -> T): T? {
    return getColumnIndex(columnName).takeIf { it != -1 }?.let { func(it) }
}
