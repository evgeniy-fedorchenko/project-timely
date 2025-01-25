package com.efedorchenko.timely.service

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import com.efedorchenko.timely.model.SyncProcess
import org.threeten.bp.Duration

object ToastHelper {

    private const val KEY_COPIED =                      "Ключ скопирован"
    private const val DATE_PASSED =                     "Эта дата уже прошла"
    const val NETWORK_ERROR =                           "Проблемы с подключением, проверте работу сети Интернет"
    private const val CANNOT_EDIT_PLANED =              "Запланированную смену нельзя редактировать!"
    private const val FINE_AMOUNT_TOO_SMALL =           "Слишком маленькая сумма"
    private const val NEEDS_FINE_DESC =                 "Необходимо ввести комментарий"
    private const val WORK_DURATION_TOO_SHORT_PATTERN = "Минимальная длина: %s часов"
    const val INCORRECT_LOGIN_DATA =                    "Неверный логин или пароль"

    /* Space operations */
    private const val CONNECT_TO_SPACE_SUCCESS =        "Вы успешно присоединились к компании"
    const val CONNECT_TO_SPACE_FILED_UNKNOWN =          "Не удалось присоединиться к компании, проверьте работу сети Интернет"
    const val CONNECT_TO_SPACE_FILED_KEY_INVALID =      "Неверный ключ доступа"
    private const val LEAVE_SPACE_SUCCESS =             "Вы успешно покинули компанию"
    private const val LEAVE_SPACE_FILED =               "Не удалось покинуть компанию, проверьте работу сети Интернет"

    /* Synchronizing data */
    const val NOT_SYNCED =                              "Проблемы с интернетом, синхронизируйте данные позже"
    const val ALL_SYNCED =                              "Все данные синхронизированы"
    private const val FILED_EVENTS_PATTERN =            "Не удалось отправить %d смен"
    private const val FILED_FINES_PATTERN =             "Не удалось отправить %d штрафов"
    private const val FILED_DOWNLOAD_NEW =              "Не удалось загрузить новые данные"
    private const val FILED_DOWNLOAD_MEMBERS =          "Не удалось обновить участников компании"

    /* Download data */
    private const val ERROR_DOWNLOAD_DATA =        "Не удалось некоторые данные, обновите данные при подключении к сети Интернет"
    private const val ERROR_DOWNLOAD_MEMBERS =     "Не удалось загрузить участников компании, обновите данные при подключении к сети Интернет"
    private const val ERROR_GET_MEMBER =           "Не удалось загрузить участника, проверьте интернет-соединение"

    /* Registration */
    private const val INVALID_NAME_ON_REG =        "Неподходящее имя. Смотри подсказку справа"
    private const val INVALID_EMAIL_ON_REG =       "Неподходящий email. Смотри подсказку справа"
    private const val INVALID_PASSWORD_ON_REG =    "Неподходящий пароль. Смотри подсказку справа"
    private const val DIFFERENT_PASSWORDS_ON_REG = "Пароли не совпадают"
    private const val INVALID_SPACE_KEY =          "Недействительный ключ пространства. Обратитесь к руководителю для его получения"
    private const val INVALID_SPACE_NAME_ON_REG =  "Неподходящее имя пространства"
    private const val INVALID_POSITION_ON_REG =    "Неподходящая должность. Смотри подсказку справа "
    const val INVALID_DATA_ON_REG =                "Упс! Некорректные данные, смотри подсказки справа"


    fun message(message: String, c: Context) =    doShow(message, c, LENGTH_SHORT)
    fun keyCopied(c: Context) =                   doShow(KEY_COPIED, c, LENGTH_SHORT)
    fun datePassed(c: Context) =                  doShow(DATE_PASSED, c, LENGTH_SHORT)
    fun networkError(c: Context) =                doShow(NETWORK_ERROR, c, LENGTH_SHORT)
    fun cannotEditPlaned(c: Context) =            doShow(CANNOT_EDIT_PLANED, c, LENGTH_SHORT)
    fun fineAmountTooSmall(c: Context?) =         doShow(FINE_AMOUNT_TOO_SMALL, c, LENGTH_SHORT)
    fun needsFineDesc(c: Context?) =              doShow(NEEDS_FINE_DESC, c, LENGTH_SHORT)
    fun incorrectLoginData(c: Context) =          doShow(INCORRECT_LOGIN_DATA, c, LENGTH_LONG)

    /* Space operations */
    fun connectToSpaceSuccess(c: Context) =       doShow(CONNECT_TO_SPACE_SUCCESS, c, LENGTH_SHORT)
    fun leaveSpaceSuccess(c: Context) =           doShow(LEAVE_SPACE_SUCCESS, c, LENGTH_LONG)
    fun leaveSpaceFiled(c: Context) =             doShow(LEAVE_SPACE_FILED, c, LENGTH_LONG)

    /* Download data */
    fun failDownloadData(c: Context) =            doShow(ERROR_DOWNLOAD_DATA, c, LENGTH_LONG)
    fun failDownloadMembers(c: Context) =         doShow(ERROR_DOWNLOAD_MEMBERS, c, LENGTH_SHORT)
    fun errorGetMember(c: Context) =              doShow(ERROR_GET_MEMBER, c, LENGTH_SHORT)

    /* Registration */
    fun invalidNameOnReg(c: Context) =            doShow(INVALID_NAME_ON_REG, c, LENGTH_LONG)
    fun invalidEmailOnReg(c: Context) =           doShow(INVALID_EMAIL_ON_REG, c, LENGTH_LONG)
    fun invalidPasswordOnReg(c: Context) =        doShow(INVALID_PASSWORD_ON_REG, c, LENGTH_LONG)
    fun passwordsAreDifferentOnReg(c: Context) =  doShow(DIFFERENT_PASSWORDS_ON_REG, c, LENGTH_LONG)
    fun invalidSpaceKey(c: Context) =             doShow(INVALID_SPACE_KEY, c, LENGTH_LONG)
    fun invalidSpaceNameOnReg(c: Context) =       doShow(INVALID_SPACE_NAME_ON_REG, c, LENGTH_LONG)
    fun invalidPositionOnReg(c: Context) =        doShow(INVALID_POSITION_ON_REG, c, LENGTH_LONG)

    fun workDurationTooShort(c: Context, minWorkDuration: Duration) {
        return doShow(WORK_DURATION_TOO_SHORT_PATTERN.format(minWorkDuration.toHours()), c, LENGTH_SHORT)
    }

    fun syncFiled(syncResult: SyncProcess.Result, context: Context) {
        val message = when {
            syncResult.eventsNotSyncCount > 0 -> FILED_EVENTS_PATTERN.format(syncResult.eventsNotSyncCount)
            syncResult.finesNotSyncCount > 0 -> FILED_FINES_PATTERN.format(syncResult.finesNotSyncCount)
            !syncResult.isRemoteDataAccepted -> FILED_DOWNLOAD_NEW
            syncResult.isRemoteMembersAccepted == SyncProcess.UpdateResult.FAIL -> FILED_DOWNLOAD_MEMBERS
//            SyncProcess.UpdateResult.SUCCESS должен быть обработан выше,
//            тк для успеха нужен анализ всего объекта SyncProcess.Result
            else -> null
        }
        doShow(message, context, LENGTH_LONG)
    }

    private fun doShow(toastText: String?, c: Context?, toastLength: Int) {
        if (toastText != null && c != null) {
            val toast = Toast.makeText(c, toastText, toastLength)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }
    }
}

