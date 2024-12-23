package com.efedorchenko.timely.service

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import org.threeten.bp.Duration

object ToastHelper {

    private const val KEY_COPIED =                      "Ключ скопирован"
    private const val NO_ACCOUNT =                      "Ну и пошел нахуй тогда"
    private const val DATE_PASSED =                     "Эта дата уже прошла"
    private const val NETWORK_ERROR =                   "Проблемы с подключением, проверте работу сети Интернет"
    private const val CANNOT_EDIT_PLANED =              "Запланированную смену нельзя редактировать!"
    private const val INCORRECT_LOGIN_DATA =            "Неверный логин или пароль"
    private const val FINE_AMOUNT_TOO_SMALL =           "Слишком маленькая сумма"
    private const val WORK_DURATION_TOO_SHORT_PATTERN = "Минимальная длина: %s часов"
    private const val INVALID_LOGIN =                   "E-mail должен быть корректным электронным адресом, длиной до 128 символов"
    private const val INVALID_PASSWORD =                "Пароль должен быть длиной от 8 до 32"


    fun message(message: String, context: Context) = showToast(message, context, LENGTH_SHORT)
    fun keyCopied(context: Context) =                showToast(KEY_COPIED, context, LENGTH_SHORT)
    fun noAccount(context: Context) =                showToast(NO_ACCOUNT, context, LENGTH_SHORT)
    fun datePassed(context: Context) =               showToast(DATE_PASSED, context, LENGTH_SHORT)
    fun networkError(context: Context) =             showToast(NETWORK_ERROR, context, LENGTH_SHORT)
    fun cannotEditPlaned(context: Context) =         showToast(CANNOT_EDIT_PLANED, context, LENGTH_SHORT)
    fun fineAmountTooSmall(context: Context) =       showToast(FINE_AMOUNT_TOO_SMALL, context, LENGTH_SHORT)
    fun incorrectLoginData(context: Context) =       showToast(INCORRECT_LOGIN_DATA, context, LENGTH_SHORT)
    fun invalidLogin(context: Context) =             showToast(INVALID_LOGIN, context, LENGTH_LONG)
    fun invalidPassword(context: Context) =          showToast(INVALID_PASSWORD, context, LENGTH_LONG)

    fun workDurationTooShort(context: Context, minWorkDuration: Duration) {
        return showToast(WORK_DURATION_TOO_SHORT_PATTERN.format(minWorkDuration), context, LENGTH_SHORT)
    }

    private fun showToast(toastText: String, context: Context, toastLength: Int) {
        val toast = Toast.makeText(context, toastText, toastLength)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }
}

