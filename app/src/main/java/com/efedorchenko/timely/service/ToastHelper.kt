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
    const val NETWORK_ERROR =                           "Проблемы с подключением, проверте работу сети Интернет"
    private const val CANNOT_EDIT_PLANED =              "Запланированную смену нельзя редактировать!"
    private const val FINE_AMOUNT_TOO_SMALL =           "Слишком маленькая сумма"
    private const val WORK_DURATION_TOO_SHORT_PATTERN = "Минимальная длина: %s часов"
    const val INCORRECT_LOGIN_DATA =                    "Неверный логин или пароль"
    const val NOT_SYNCHRONIZED =                        "Проблемы с интернетом, синхронизируйте данные позже"
    const val ALL_SYNCED =                              "Все данные синхронизированы"
    private const val FILED_ALL_PATTERN =               "Не удалось отправить %d смен и %d штрафов"
    private const val FILED_EVENTS_PATTERN =            "Не удалось отправить %s смен"
    private const val FILED_FINES_PATTERN =             "Не удалось отправить %s штрафов"

    /* Download data */
    const val ERROR_ENC_PROFILE =                  "Не найдены данные профиля, необходимо заново авторизоваться"
    const val ERROR_DOWNLOAD_DATA =                "Не удалось данные смен и штрафов, обновите данные при подключении к сети Интернет"
    const val ERROR_DOWNLOAD_FINES =               "Не удалось данные штрафов, обновите данные при подключении к сети Интернет"
    private const val ERROR_DOWNLOAD_MEMBERS =     "Не удалось загрузить участников компании, обновите данные при подключении к сети Интернет"

    /* Registration */
    private const val INVALID_NAME_ON_REG =        "Неподходящее имя. Смотри подсказку справа"
    private const val INVALID_EMAIL_ON_REG =       "Неподходящий email. Смотри подсказку справа"
    private const val INVALID_PASSWORD_ON_REG =    "Неподходящий пароль. Смотри подсказку справа"
    private const val DIFFERENT_PASSWORDS_ON_REG = "Пароли не совпадают"
    private const val INVALID_SPACE_KEY_ON_REG =   "Недействительный ключ пространства. Обратитесь к руководителю для его получения"
    private const val INVALID_SPACE_NAME_ON_REG =  "Неподходящее имя пространства"
    private const val INVALID_POSITION_ON_REG =    "Неподходящая должность. Смотри подсказку справа "
    const val INVALID_DATA_ON_REG =                "Упс! Некорректные данные, смотри подсказки справа"


    fun message(message: String, c: Context) =    doShow(message, c, LENGTH_SHORT)
    fun keyCopied(c: Context) =                   doShow(KEY_COPIED, c, LENGTH_SHORT)
    fun datePassed(c: Context) =                  doShow(DATE_PASSED, c, LENGTH_SHORT)
    fun networkError(c: Context) =                doShow(NETWORK_ERROR, c, LENGTH_SHORT)
    fun cannotEditPlaned(c: Context) =            doShow(CANNOT_EDIT_PLANED, c, LENGTH_SHORT)
    fun fineAmountTooSmall(c: Context) =          doShow(FINE_AMOUNT_TOO_SMALL, c, LENGTH_SHORT)
    fun incorrectLoginData(c: Context) =          doShow(INCORRECT_LOGIN_DATA, c, LENGTH_LONG)

    /* Download data */
    fun failDownloadMembers(c: Context) =         doShow(ERROR_DOWNLOAD_MEMBERS, c, LENGTH_SHORT)

    /* Registration */
    fun invalidNameOnReg(c: Context) =            doShow(INVALID_NAME_ON_REG, c, LENGTH_LONG)
    fun invalidEmailOnReg(c: Context) =           doShow(INVALID_EMAIL_ON_REG, c, LENGTH_LONG)
    fun invalidPasswordOnReg(c: Context) =        doShow(INVALID_PASSWORD_ON_REG, c, LENGTH_LONG)
    fun passwordsAreDifferentOnReg(c: Context) =  doShow(DIFFERENT_PASSWORDS_ON_REG, c, LENGTH_LONG)
    fun invalidSpaceKeyOnReg(c: Context) =        doShow(INVALID_SPACE_KEY_ON_REG, c, LENGTH_LONG)
    fun invalidSpaceNameOnReg(c: Context) =       doShow(INVALID_SPACE_NAME_ON_REG, c, LENGTH_LONG)
    fun invalidPositionOnReg(c: Context) =        doShow(INVALID_POSITION_ON_REG, c, LENGTH_LONG)

    fun workDurationTooShort(c: Context, minWorkDuration: Duration) {
        return doShow(WORK_DURATION_TOO_SHORT_PATTERN.format(minWorkDuration.toHours()), c, LENGTH_SHORT)
    }

    fun syncFiled(eventsCount: Int, finesCount: Int, context: Context) {
        val message = when {
            (eventsCount != 0 && finesCount != 0) -> FILED_ALL_PATTERN.format(eventsCount, finesCount)
            eventsCount != 0 -> FILED_EVENTS_PATTERN.format(eventsCount)
            else -> FILED_FINES_PATTERN.format(finesCount)
        }
        doShow(message, context, LENGTH_LONG)
    }

    private fun doShow(toastText: String, c: Context, toastLength: Int) {
        val toast = Toast.makeText(c, toastText, toastLength)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }
}

