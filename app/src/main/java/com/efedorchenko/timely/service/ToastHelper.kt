package com.efedorchenko.timely.service

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import org.threeten.bp.Duration

object ToastHelper {

    private const val KEY_COPIED =                      "Ключ скопирован"
    private const val DATE_PASSED =                     "Эта дата уже прошла"
    private const val NETWORK_ERROR =                   "Проблемы с подключением, проверте работу сети Интернет"
    private const val CANNOT_EDIT_PLANED =              "Запланированную смену нельзя редактировать!"
    private const val INCORRECT_LOGIN_DATA =            "Неверный логин или пароль"
    private const val FINE_AMOUNT_TOO_SMALL =           "Слишком маленькая сумма"
    private const val WORK_DURATION_TOO_SHORT_PATTERN = "Минимальная длина: %s часов"
    private const val INVALID_LOGIN =                   "E-mail должен быть корректным электронным адресом, длиной до 128 символов"
    private const val INVALID_PASSWORD =                "Пароль должен быть длиной от 8 до 32"
    private const val INVALID_NAME_ON_REG =             "Неподходящее имя. Смотри подсказку справа под знаком вопроса"
    private const val INVALID_EMAIL_ON_REG =            "Неподходящий email. Смотри подсказку справа под знаком вопроса"
    private const val INVALID_PASSWORD_ON_REG =         "Неподходящий пароль. Смотри подсказку справа под знаком вопроса"
    private const val DIFFERENT_PASSWORDS_ON_REG =      "Пароли не соответствуют друг другу"
    private const val INVALID_SPACE_KEY_ON_REG =        "Недействительный ключ пространства. Обратитесь к руководителю для его получения"


    fun message(message: String, c: Context) =    showToast(message, c, LENGTH_SHORT)
    fun keyCopied(c: Context) =                   showToast(KEY_COPIED, c, LENGTH_SHORT)
    fun datePassed(c: Context) =                  showToast(DATE_PASSED, c, LENGTH_SHORT)
    fun networkError(c: Context) =                showToast(NETWORK_ERROR, c, LENGTH_SHORT)
    fun cannotEditPlaned(c: Context) =            showToast(CANNOT_EDIT_PLANED, c, LENGTH_SHORT)
    fun fineAmountTooSmall(c: Context) =          showToast(FINE_AMOUNT_TOO_SMALL, c, LENGTH_SHORT)
    fun incorrectLoginData(c: Context) =          showToast(INCORRECT_LOGIN_DATA, c, LENGTH_SHORT)
    fun invalidLogin(c: Context) =                showToast(INVALID_LOGIN, c, LENGTH_LONG)
    fun invalidPassword(c: Context) =             showToast(INVALID_PASSWORD, c, LENGTH_LONG)
    fun invalidNameOnReg(c: Context) =            showToast(INVALID_NAME_ON_REG, c, LENGTH_SHORT)
    fun invalidEmailOnReg(c: Context) =           showToast(INVALID_EMAIL_ON_REG, c, LENGTH_SHORT)
    fun invalidPasswordOnReg(c: Context) =        showToast(INVALID_PASSWORD_ON_REG, c, LENGTH_SHORT)
    fun passwordsAreDifferentOnReg(c: Context) =  showToast(DIFFERENT_PASSWORDS_ON_REG, c, LENGTH_SHORT)
    fun invalidSpaceKeyOnReg(c: Context) =        showToast(INVALID_SPACE_KEY_ON_REG, c, LENGTH_SHORT)


    fun workDurationTooShort(c: Context, minWorkDuration: Duration) {
        return showToast(WORK_DURATION_TOO_SHORT_PATTERN.format(minWorkDuration), c, LENGTH_SHORT)
    }

    private fun showToast(toastText: String, c: Context, toastLength: Int) {
        val toast = Toast.makeText(c, toastText, toastLength)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }
}

