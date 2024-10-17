package com.efedorchenko.timely.service

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import org.threeten.bp.Duration

object ToastHelper {

    private const val KEY_CPOIED =                      "Ключ скопирован"
    private const val NO_ACCOUNT =                      "Ну и пошел нахуй тогда"
    private const val DATE_PASSSED =                    "Эта дата уже прошла"
    private const val NETWORK_ERROR =                   "Проблемы с подключением, проверте работу сети Интернет"
    private const val CANNOT_EDIT_PLANED =              "Запланированную смену нельзя редактировать!"
    private const val INCORRECT_LOGIN_DATA =            "Неверный логин или пароль"
    private const val FINE_AMOUNT_TOO_SMALL =           "Слишком маленькая сумма"
    private const val WORK_DURATION_TOO_SHORT_PATTERN = "Минимальная длина: %s часов"


    fun keyCopied(context: Context) =          showToast(KEY_CPOIED, context)
    fun noAccount(context: Context) =          showToast(NO_ACCOUNT, context)
    fun datePassed(context: Context) =         showToast(DATE_PASSSED, context)
    fun networkError(context: Context) =       showToast(NETWORK_ERROR, context)
    fun cannotEditPlaned(context: Context) =   showToast(CANNOT_EDIT_PLANED, context)
    fun fineAmountTooSmall(context: Context) = showToast(FINE_AMOUNT_TOO_SMALL, context)

    fun incorrectLoginData(context: Context) = showToast(INCORRECT_LOGIN_DATA, context)

    fun workDurationTooShort(context: Context, minWorkDuration: Duration) {
        return showToast(WORK_DURATION_TOO_SHORT_PATTERN.format(minWorkDuration), context)
    }

    private fun showToast(toastText: String, context: Context) {
        val toast = Toast.makeText(context, toastText, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }
}

